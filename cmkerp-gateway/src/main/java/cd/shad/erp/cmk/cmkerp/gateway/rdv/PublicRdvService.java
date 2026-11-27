package cd.shad.erp.cmk.cmkerp.gateway.rdv;

import static org.springframework.http.HttpStatus.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "cmk.rdv.enabled", havingValue = "true")
class PublicRdvService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String SLOT_QUERY = """
      SELECT c.id, d.nom AS medecin, s.libelle AS service, l.libelle AS lieu, c.debut, c.fin
      FROM erp_rdv_creneaux c JOIN erp_medecins d ON d.id = c.fk_medecin AND d.actif = 1
      JOIN erp_services s ON s.id = c.fk_service AND s.actif = 1
      JOIN erp_locations l ON l.id = c.fk_location AND l.actif = 1
      """;
  private static final String BOOKING_QUERY = """
      SELECT r.reference, r.statut, r.patient_nom, r.patient_prenom, r.fk_creneau, c.debut, c.fin,
             d.nom AS medecin, s.libelle AS service, l.libelle AS lieu
      FROM erp_rendez_vous r JOIN erp_rdv_creneaux c ON c.id = r.fk_creneau
      JOIN erp_medecins d ON d.id = c.fk_medecin JOIN erp_services s ON s.id = c.fk_service
      JOIN erp_locations l ON l.id = c.fk_location
      """;
  private final NamedParameterJdbcTemplate primary;
  private final ObjectProvider<NamedParameterJdbcTemplate> mediline;
  private final ObjectProvider<JavaMailSender> mail;
  private final TransactionTemplate tx;
  private final String origin;
  private final String sender;

  PublicRdvService(@Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate primary,
      @Qualifier("medilineNamedParameterJdbcTemplate") ObjectProvider<NamedParameterJdbcTemplate> mediline,
      ObjectProvider<JavaMailSender> mail,
      @Value("${cmk.rdv.public-origin:https://rdv.cmkerp.com}") String origin,
      @Value("${cmk.rdv.sender:}") String sender) {
    this.primary = primary;
    this.mediline = mediline;
    this.mail = mail;
    this.origin = origin.replaceFirst("/$", "");
    this.sender = sender;
    this.tx = new TransactionTemplate(new JdbcTransactionManager(primary.getJdbcTemplate().getDataSource()));
  }

  @Bean
  ApplicationRunner rendezVousSchema() {
    return args -> primary.getJdbcTemplate().execute((ConnectionCallback<Void>) connection -> {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V48__erp_rendez_vous_public.sql"));
      return null;
    });
  }

  record Slot(long id, String medecin, String service, String lieu, Instant debut, Instant fin) {}
  record Booking(String reference, String statut, String nom, String prenom, long creneauId,
      Instant debut, Instant fin, String medecin, String service, String lieu) {}
  record Session(String hash, String email, String purpose, Long creneauId) {
    boolean account() { return "COMPTE".equals(purpose); }
  }
  record Access(String rawToken, String purpose, Long creneauId, String email) {}
  record Profile(String email, boolean dossierLie) {}
  record StaffBooking(String reference, String statut, String nom, String prenom, String ipp,
      Instant debut, String medecin, String service, String lieu) {}

  List<Slot> slots() {
    return primary.query(SLOT_QUERY + """
        WHERE c.public_visible = TRUE AND c.debut > :now AND c.debut < :limit
          AND NOT EXISTS (SELECT 1 FROM erp_rendez_vous r WHERE r.fk_creneau_actif = c.id)
        ORDER BY c.debut, c.id LIMIT 500
        """, new MapSqlParameterSource("now", now()).addValue("limit", now().plusDays(60)), (rs, row) -> new Slot(
            rs.getLong("id"), rs.getString("medecin"), rs.getString("service"), rs.getString("lieu"),
            utc(rs.getObject("debut", LocalDateTime.class)), utc(rs.getObject("fin", LocalDateTime.class))));
  }

  void sendLink(String rawEmail, String purpose, Long slot, String source) {
    if (!List.of("VISITE", "COMPTE").contains(purpose) || ("VISITE".equals(purpose) && slot == null)
        || ("COMPTE".equals(purpose) && slot != null)) throw error(BAD_REQUEST, "Vérifiez la demande d’accès.");
    var mailer = mail.getIfAvailable();
    if (mailer == null || sender.isBlank()) throw error(SERVICE_UNAVAILABLE, "L’envoi des liens est indisponible.");
    String email = rawEmail.trim().toLowerCase(Locale.ROOT);
    if (!StandardCharsets.US_ASCII.newEncoder().canEncode(email)) throw error(BAD_REQUEST, "Adresse e-mail invalide.");
    String code = token();
    long minute = Instant.now().getEpochSecond() / 60;
    if (!Boolean.TRUE.equals(tx.execute(status -> {
      var rate = new MapSqlParameterSource("source", hash(source)).addValue("bucket", minute / 10);
      primary.update("""
          INSERT INTO erp_rdv_limites (source_hash, bucket) VALUES (:source, :bucket)
          ON DUPLICATE KEY UPDATE tentatives = tentatives + 1
          """, rate);
      Integer attempts = primary.queryForObject("""
          SELECT tentatives FROM erp_rdv_limites WHERE source_hash = :source AND bucket = :bucket
          """, rate, Integer.class);
      return attempts != null && attempts <= 10;
    }))) throw error(TOO_MANY_REQUESTS, "Trop de demandes. Réessayez dans quelques minutes.");
    try {
      tx.executeWithoutResult(status -> {
        if (slot != null) requireAvailable(slot);
        primary.update("""
            INSERT INTO erp_rdv_liens (token_hash,email,purpose,fk_creneau,valid_until,throttle_bucket)
            VALUES (:hash,:email,:purpose,:slot,:expiry,:minute)
            """, new MapSqlParameterSource("hash", hash(code)).addValue("email", email).addValue("purpose", purpose)
            .addValue("slot", slot).addValue("expiry", now().plusMinutes(15)).addValue("minute", minute / 2));
      });
    } catch (DuplicateKeyException duplicate) {
      throw error(TOO_MANY_REQUESTS, "Un lien vient d’être demandé. Vérifiez votre messagerie.");
    }
    var message = new SimpleMailMessage();
    message.setFrom(sender);
    message.setTo(email);
    message.setSubject("Votre lien d’accès CMK Rendez-vous");
    message.setText("Ouvrez ce lien dans les 15 minutes pour continuer sur CMK Rendez-vous :\n"
        + origin + "/#verification=" + code + "\n\nSi vous n’avez rien demandé, ignorez cet e-mail.");
    try {
      mailer.send(message);
    } catch (MailException failure) {
      tx.executeWithoutResult(status -> primary.update(
          "DELETE FROM erp_rdv_liens WHERE token_hash = :hash", Map.of("hash", hash(code))));
      throw error(SERVICE_UNAVAILABLE, "Le lien n’a pas pu être envoyé. Réessayez dans un instant.");
    }
  }

  Access verifyLink(String rawToken) {
    var key = tokenParams(rawToken);
    return tx.execute(status -> {
      var sessions = primary.query("""
          SELECT email,purpose,fk_creneau FROM erp_rdv_liens
          WHERE token_hash = :hash AND valid_until > :now AND consumed_at IS NULL FOR UPDATE
          """, key, (rs, row) -> new Session(null, rs.getString("email"), rs.getString("purpose"),
            rs.getObject("fk_creneau", Long.class)));
      if (sessions.isEmpty()) throw error(UNAUTHORIZED, "Ce lien a expiré ou a déjà été utilisé. Demandez un nouveau lien.");
      var link = sessions.getFirst();
      if (link.account()) primary.update("""
          INSERT INTO erp_rdv_comptes (email) VALUES (:email) ON DUPLICATE KEY UPDATE email = email
          """, Map.of("email", link.email()));
      String rawSession = token();
      primary.update("""
          INSERT INTO erp_rdv_sessions (token_hash,email,purpose,fk_creneau,valid_until)
          VALUES (:hash,:email,:purpose,:slot,:expiry)
          """, new MapSqlParameterSource("hash", hash(rawSession)).addValue("email", link.email())
          .addValue("purpose", link.purpose()).addValue("slot", link.creneauId())
          .addValue("expiry", now().plusHours(link.account() ? 12 : 1)));
      primary.update("UPDATE erp_rdv_liens SET consumed_at = :now WHERE token_hash = :hash", key);
      return new Access(rawSession, link.purpose(), link.creneauId(), link.email());
    });
  }

  Session session(String raw, boolean accountRequired) {
    var found = primary.query("""
        SELECT token_hash,email,purpose,fk_creneau FROM erp_rdv_sessions
        WHERE token_hash = :hash AND valid_until > :now AND revoked_at IS NULL
        """, tokenParams(raw), (rs, row) -> new Session(rs.getString("token_hash"), rs.getString("email"),
          rs.getString("purpose"), rs.getObject("fk_creneau", Long.class)));
    if (found.isEmpty() || (accountRequired && !found.getFirst().account())) {
      throw error(UNAUTHORIZED, "Ouvrez un lien de connexion pour continuer.");
    }
    return found.getFirst();
  }

  void logout(String raw) {
    if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) return;
    tx.executeWithoutResult(status -> primary.update(
        "UPDATE erp_rdv_sessions SET revoked_at = :now WHERE token_hash = :hash", tokenParams(raw)));
  }

  Booking book(Session session, long slot, UUID requestId, String name, String firstName, boolean forSelf) {
    if (!session.account() && !Long.valueOf(slot).equals(session.creneauId())) {
      throw error(FORBIDDEN, "Ce lien est lié au créneau sélectionné lors de la vérification.");
    }
    return tx.execute(status -> {
      var params = bookingParams(session.email(), slot).addValue("request", requestId.toString())
          .addValue("session", session.hash()).addValue("nom", name.strip()).addValue("prenom", firstName.strip());
      if (primary.queryForList("""
          SELECT token_hash FROM erp_rdv_sessions
          WHERE token_hash = :session AND revoked_at IS NULL AND valid_until > :now FOR UPDATE
          """, params.addValue("now", now()), String.class).isEmpty()) {
        throw error(UNAUTHORIZED, "Ouvrez un nouveau lien pour continuer.");
      }
      var replay = bookings(" WHERE r.request_id = :request AND r.email = :email", params);
      if (!replay.isEmpty()) {
        var previous = replay.getFirst();
        if (previous.creneauId() != slot || !previous.nom().equals(name.strip())
            || !previous.prenom().equals(firstName.strip())) throw error(CONFLICT, "Ce rendez-vous a déjà été enregistré.");
        return previous;
      }
      if (!session.account() && !bookings(" WHERE r.session_hash = :session", params).isEmpty()) {
        throw error(CONFLICT, "Un rendez-vous a déjà été enregistré avec ce lien.");
      }
      requireAvailable(slot);
      String reference = HexFormat.of().formatHex(RANDOM.generateSeed(8)).toUpperCase(Locale.ROOT);
      params.addValue("reference", reference).addValue("ipp", session.account() && forSelf ? linkedIpp(session.email()) : null);
      try {
        primary.update("""
            INSERT INTO erp_rendez_vous
            (reference,request_id,session_hash,fk_creneau,fk_creneau_actif,email,patient_nom,patient_prenom,mediline_ipp)
            VALUES (:reference,:request,:session,:slot,:slot,:email,:nom,:prenom,:ipp)
            """, params);
      } catch (DuplicateKeyException duplicate) {
        throw error(CONFLICT, "Ce créneau n’est plus disponible. Sélectionnez une autre heure.");
      }
      return bookings(" WHERE r.reference = :reference AND r.email = :email", params).getFirst();
    });
  }

  List<Booking> list(Session session) {
    return bookings(" WHERE r.email = :email ORDER BY c.debut DESC, r.id DESC LIMIT 150",
        new MapSqlParameterSource("email", session.email()));
  }

  List<StaffBooking> staffBookings(LocalDate day, Long doctor) {
    var zone = ZoneId.of("Africa/Kinshasa");
    var from = LocalDateTime.ofInstant(day.atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
    var to = LocalDateTime.ofInstant(day.plusDays(1).atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
    return primary.query("""
        SELECT r.reference, r.statut, r.patient_nom, r.patient_prenom, r.mediline_ipp, c.debut,
               d.nom AS medecin, s.libelle AS service, l.libelle AS lieu
        FROM erp_rendez_vous r JOIN erp_rdv_creneaux c ON c.id = r.fk_creneau
        JOIN erp_medecins d ON d.id = c.fk_medecin JOIN erp_services s ON s.id = c.fk_service
        JOIN erp_locations l ON l.id = c.fk_location
        WHERE c.debut >= :from AND c.debut < :to
        """ + (doctor == null ? "" : " AND c.fk_medecin = :doctor") + " ORDER BY c.debut, r.id",
        new MapSqlParameterSource("from", from).addValue("to", to).addValue("doctor", doctor), (rs, row) ->
            new StaffBooking(rs.getString("reference"), rs.getString("statut"), rs.getString("patient_nom"),
                rs.getString("patient_prenom"), rs.getString("mediline_ipp"),
                utc(rs.getObject("debut", LocalDateTime.class)), rs.getString("medecin"),
                rs.getString("service"), rs.getString("lieu")));
  }

  Booking guestReceipt(Session session) {
    if (session.account()) throw error(FORBIDDEN, "Consultez vos réservations dans votre espace.");
    return bookings(" WHERE r.session_hash = :session AND r.email = :email",
        new MapSqlParameterSource("session", session.hash()).addValue("email", session.email()))
        .stream().findFirst().orElse(null);
  }

  Booking update(Session session, String reference, Long nextSlot) {
    return tx.execute(status -> {
      var params = bookingParams(session.email(), nextSlot).addValue("reference", reference);
      var matches = bookings(" WHERE r.reference = :reference AND r.email = :email FOR UPDATE", params);
      if (matches.isEmpty()) throw error(NOT_FOUND, "Rendez-vous introuvable.");
      var booking = matches.getFirst();
      if (nextSlot == null && "ANNULE".equals(booking.statut())) return booking;
      if (!"CONFIRME".equals(booking.statut()) || !booking.debut().isAfter(Instant.now())) {
        throw error(CONFLICT, "Ce rendez-vous ne peut plus être modifié en ligne.");
      }
      if (nextSlot == null) {
        primary.update("""
            UPDATE erp_rendez_vous SET statut = 'ANNULE', cancelled_at = :now, fk_creneau_actif = NULL
            WHERE reference = :reference AND email = :email
            """, params.addValue("now", now()));
      } else if (nextSlot != booking.creneauId()) {
        requireAvailable(nextSlot);
        try {
          primary.update("""
              UPDATE erp_rendez_vous SET fk_creneau = :slot, fk_creneau_actif = :slot
              WHERE reference = :reference AND email = :email
              """, params);
        } catch (DuplicateKeyException duplicate) {
          throw error(CONFLICT, "Le nouveau créneau n’est plus disponible. Votre rendez-vous est conservé.");
        }
      }
      return bookings(" WHERE r.reference = :reference AND r.email = :email", params).getFirst();
    });
  }

  Profile profile(Session session) {
    return new Profile(session.email(), linkedIpp(session.email()) != null);
  }

  Profile linkMediline(Session session, String ipp, LocalDate birthDate, String source) {
    var reference = ipp.strip().toUpperCase(Locale.ROOT);
    if (!reference.matches("[A-Z0-9-]{4,25}")) throw error(BAD_REQUEST, "Vérifiez le format de votre IPP.");
    limitLinking(session, source);
    var jdbc = mediline.getIfAvailable();
    if (jdbc == null) throw error(SERVICE_UNAVAILABLE, "La liaison au dossier est momentanément indisponible.");
    var emails = jdbc.queryForList("""
        SELECT EMAIL FROM t_personne
        WHERE CODE = :ipp AND PERSONNE_PHYSIQUE = 1 AND DATE_NAISSANCE = :birth LIMIT 2
        """, Map.of("ipp", reference, "birth", birthDate), String.class);
    if (emails.size() != 1 || emails.getFirst() == null || !session.email().equalsIgnoreCase(emails.getFirst().strip())) {
      throw error(UNPROCESSABLE_ENTITY, "Le dossier n’a pas pu être associé. La Réception peut vérifier votre identité.");
    }
    try {
      return tx.execute(status -> {
        String existing = primary.queryForObject(
            "SELECT mediline_ipp FROM erp_rdv_comptes WHERE email = :email FOR UPDATE",
            Map.of("email", session.email()), String.class);
        if (existing != null && !existing.equalsIgnoreCase(reference)) {
          throw error(CONFLICT, "Un dossier est déjà associé à ce compte.");
        }
        primary.update("""
            UPDATE erp_rdv_comptes SET mediline_ipp = :ipp, linked_at = :now WHERE email = :email
            """, new MapSqlParameterSource("ipp", reference).addValue("now", now()).addValue("email", session.email()));
        return new Profile(session.email(), true);
      });
    } catch (DuplicateKeyException alreadyLinked) {
      throw error(UNPROCESSABLE_ENTITY, "Le dossier n’a pas pu être associé. La Réception peut vérifier votre identité.");
    }
  }

  void addSlot(long doctor, long service, long site, OffsetDateTime from, OffsetDateTime to, Long actor) {
    LocalDateTime begin = LocalDateTime.ofInstant(from.toInstant(), ZoneOffset.UTC).withNano(0);
    LocalDateTime end = LocalDateTime.ofInstant(to.toInstant(), ZoneOffset.UTC).withNano(0);
    if (!end.isAfter(begin) || !begin.isAfter(now()) || end.isAfter(begin.plusHours(8))) {
      throw error(BAD_REQUEST, "Vérifiez le début et la fin du créneau.");
    }
    tx.executeWithoutResult(status -> {
      var params = new MapSqlParameterSource("doctor", doctor).addValue("service", service)
          .addValue("site", site).addValue("begin", begin).addValue("end", end).addValue("actor", actor);
      if (primary.queryForList("SELECT id FROM erp_medecins WHERE id = :doctor AND actif = 1 FOR UPDATE",
          params, Long.class).isEmpty()) throw error(NOT_FOUND, "Médecin actif introuvable.");
      if (!Boolean.TRUE.equals(primary.queryForObject("""
          SELECT EXISTS (SELECT 1 FROM erp_services WHERE id = :service AND actif = 1)
            AND EXISTS (SELECT 1 FROM erp_locations WHERE id = :site AND actif = 1)
          """, params, Boolean.class))) throw error(NOT_FOUND, "Service ou lieu actif introuvable.");
      var overlap = primary.queryForList("""
          SELECT id FROM erp_rdv_creneaux WHERE fk_medecin = :doctor AND debut < :end AND fin > :begin FOR UPDATE
          """, params, Long.class);
      if (!overlap.isEmpty()) throw error(CONFLICT, "Ce médecin a déjà un créneau pendant cette période.");
      primary.update("""
          INSERT INTO erp_rdv_creneaux (fk_medecin,fk_service,fk_location,debut,fin,public_visible,created_by)
          VALUES (:doctor,:service,:site,:begin,:end,TRUE,:actor)
          """, params);
    });
  }

  @Scheduled(cron = "0 27 3 * * *", zone = "Africa/Kinshasa")
  void purgeExpiredAccess() {
    tx.executeWithoutResult(status -> {
      primary.update("DELETE FROM erp_rdv_liens WHERE valid_until < :date", Map.of("date", now().minusDays(1)));
      primary.update("DELETE FROM erp_rdv_sessions WHERE valid_until < :date", Map.of("date", now().minusDays(1)));
      primary.update("DELETE FROM erp_rdv_limites WHERE bucket < :bucket",
          Map.of("bucket", Instant.now().getEpochSecond() / 600 - 144));
    });
  }

  private void requireAvailable(long slot) {
    var args = new MapSqlParameterSource("slot", slot).addValue("now", now());
    var available = primary.queryForList("""
        SELECT c.id FROM erp_rdv_creneaux c JOIN erp_medecins d ON d.id = c.fk_medecin AND d.actif = 1
        JOIN erp_services s ON s.id = c.fk_service AND s.actif = 1
        JOIN erp_locations l ON l.id = c.fk_location AND l.actif = 1
        WHERE c.id = :slot AND c.public_visible = TRUE AND c.debut > :now FOR UPDATE
        """, args, Long.class);
    if (available.isEmpty() || !primary.queryForList("""
        SELECT id FROM erp_rendez_vous WHERE fk_creneau_actif = :slot FOR UPDATE
        """, args, Long.class).isEmpty()) {
      throw error(CONFLICT, "Ce créneau n’est plus disponible. Sélectionnez une autre heure.");
    }
  }

  private void limitLinking(Session session, String source) {
    var args = new MapSqlParameterSource("source", hash("link:" + session.email() + ":" + source))
        .addValue("bucket", Instant.now().getEpochSecond() / 600);
    if (!Boolean.TRUE.equals(tx.execute(status -> {
      primary.update("""
          INSERT INTO erp_rdv_limites (source_hash,bucket) VALUES (:source,:bucket)
          ON DUPLICATE KEY UPDATE tentatives = tentatives + 1
          """, args);
      return primary.queryForObject("SELECT tentatives FROM erp_rdv_limites WHERE source_hash=:source AND bucket=:bucket",
          args, Integer.class) <= 5;
    }))) throw error(TOO_MANY_REQUESTS, "Trop de tentatives de liaison. Réessayez dans quelques minutes.");
  }

  private String linkedIpp(String email) {
    return primary.queryForObject("SELECT mediline_ipp FROM erp_rdv_comptes WHERE email = :email",
        Map.of("email", email), String.class);
  }

  private List<Booking> bookings(String filter, MapSqlParameterSource params) {
    return primary.query(BOOKING_QUERY + filter, params, (rs, row) -> new Booking(
        rs.getString("reference"), rs.getString("statut"), rs.getString("patient_nom"), rs.getString("patient_prenom"),
        rs.getLong("fk_creneau"), utc(rs.getObject("debut", LocalDateTime.class)),
        utc(rs.getObject("fin", LocalDateTime.class)), rs.getString("medecin"), rs.getString("service"), rs.getString("lieu")));
  }

  private static MapSqlParameterSource bookingParams(String email, Long slot) {
    return new MapSqlParameterSource("email", email).addValue("slot", slot);
  }

  private static MapSqlParameterSource tokenParams(String raw) {
    if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw error(UNAUTHORIZED, "Lien ou session invalide.");
    return new MapSqlParameterSource("hash", hash(raw)).addValue("now", now());
  }

  private static String token() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String hash(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
  private static Instant utc(LocalDateTime time) { return time.toInstant(ZoneOffset.UTC); }
  private static ResponseStatusException error(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }
}
