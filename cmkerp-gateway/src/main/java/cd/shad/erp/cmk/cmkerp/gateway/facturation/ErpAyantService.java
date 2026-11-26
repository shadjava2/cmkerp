package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * Service métier Ayants droit : persistance {@code erp_ayants_droit} + intention Outbox
 * {@code TAYANTDROIT}. Whitelist Beach reste {@code TCONTRAT} jusqu'à autorisation explicite.
 */
@Service
public class ErpAyantService {

  private static final Logger log = LoggerFactory.getLogger(ErpAyantService.class);
  private static final String ENTITY = "TAYANTDROIT";
  private static final Set<String> RELATIONS = Set.of("A", "C", "E", "P", "O");
  private static final int BEACH_MATRICULE_MAX = 10;

  private final ErpFacturationRelationalStore store;
  private final ClinicReferentialRepository referentialRepository;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<ClinicBeachOutboxWorker> outboxWorker;

  public ErpAyantService(
      ErpFacturationRelationalStore store,
      ClinicReferentialRepository referentialRepository,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper,
      ObjectProvider<ClinicBeachOutboxWorker> outboxWorker) {
    this.store = store;
    this.referentialRepository = referentialRepository;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
    this.outboxWorker = outboxWorker;
  }

  public Map<String, Object> list(
      String q,
      Long societeId,
      Long contratId,
      Long locationId,
      Boolean actif,
      String patientLinkStatus,
      String syncStatus,
      int page,
      int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    Map<String, Object> pageData = store.searchAyantsFiltered(
        q, societeId, contratId, locationId, actif, patientLinkStatus, syncStatus,
        safeSize, safePage * safeSize);
    Map<String, Object> result = new LinkedHashMap<>(pageData);
    result.put("page", safePage);
    result.put("size", safeSize);
    return result;
  }

  public Map<String, Object> get(long id) {
    return store.findAyantDetail(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ayant droit introuvable."));
  }

  public Map<String, Object> meta() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("relations", Map.of(
        "A", "Assuré / Titulaire",
        "C", "Conjoint",
        "E", "Enfant",
        "P", "Parent",
        "O", "Autre"));
    result.put("sources", Map.of("CM", "CM (valeur historique dominante)"));
    result.put("nationalites", Map.of(
        "C", "Congolaise",
        "E", "Étrangère"));
    // Petits référentiels ERP locaux — jamais CLINIQUE Beach.
    List<Map<String, Object>> locations = store.listActiveLocationsForPicker();
    List<Map<String, Object>> contrats = store.listActiveContratsForPicker();
    result.put("locations", locations);
    result.put("contrats", contrats);
    result.put("locationsTotal", locations.size());
    result.put("contratsTotal", contrats.size());
    result.put("beachMatriculeMaxLen", BEACH_MATRICULE_MAX);
    result.put("beachWriteEnabled", writeGuard.beachEntityWriteEnabled(ENTITY));
    result.put("writeEntitiesHint", "TCONTRAT,TAYANTDROIT");
    return result;
  }

  public Map<String, Object> lookupLocations() {
    List<Map<String, Object>> items = store.listActiveLocationsForPicker();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("total", items.size());
    return result;
  }

  public Map<String, Object> lookupContrats() {
    List<Map<String, Object>> items = store.listActiveContratsForPicker();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("total", items.size());
    return result;
  }

  public Map<String, Object> lookupSocietes(String q, int page, int size) {
    return store.searchSocietesForPicker(q, page, size);
  }

  @Transactional
  public Map<String, Object> create(Map<String, Object> body, Long userId) {
    writeGuard.assertChWriteForbidden();
    String medilineIpp = required(body, "medilineIpp", "La sélection d'un patient Mediline est obligatoire.");
    String nomComplet = required(body, "nomComplet", "L'identité Mediline est obligatoire.");
    Long societeId = requiredLong(body, "societeId", "La société est obligatoire.");
    Long contratId = requiredLong(body, "contratId", "Le contrat est obligatoire.");
    String relation = normalizeRelation(str(body.get("relation")));
    Long locationId = requiredLong(body, "locationId", "La location est obligatoire.");

    String matricule = resolveBeachMatricule(medilineIpp, str(body.get("matriculeHistorique")));
    if (store.findAyantByMatricule(matricule).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Un ayant droit existe déjà pour ce matricule (" + matricule + ").");
    }
    if (store.findAyantByMedilineIpp(medilineIpp).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Ce patient Mediline dispose déjà d'une affiliation.");
    }

    var societe = store.findSocieteDetail(societeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Société introuvable."));
    var contrat = store.findContratDetail(contratId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrat introuvable."));
    var location = store.findLocationDetail(locationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location introuvable."));

    HolderResolution holder = resolveHolder(relation, matricule, body);
    String assurance = yn(body.get("assurance"), "N");
    String covid = yn(body.get("covid"), "N");
    String source = firstNonBlank(str(body.get("source")), "CM");
    String sexe = normalizeSexe(str(body.get("sexe")));
    String nationalite = normalizeNationalite(str(body.get("nationalite")));
    LocalDate dateNaissance = parseDate(body.get("dateNaissance"));
    Long medilinePatientId = longOrNull(body.get("medilinePatientId"));

    String syncStatus = writeGuard.beachEntityWriteEnabled(ENTITY) ? "PENDING" : "SUSPENDED";
    String fingerprint = fingerprint(matricule, nomComplet, societe.get("code"), contrat.get("code"), relation);
    long id = store.insertAyantErp(
        matricule,
        nomComplet,
        sexe,
        dateNaissance,
        nationalite,
        relation,
        holder.codeFamille(),
        holder.fkTitulaire(),
        holder.titulaireIpp(),
        societeId,
        str(societe.get("code")),
        contratId,
        str(contrat.get("code")),
        medilinePatientId,
        medilineIpp,
        assurance,
        covid,
        locationId,
        str(location.get("code")),
        source,
        syncStatus,
        fingerprint,
        userId);

    Map<String, Object> beachFields = beachPayload(
        matricule, nomComplet, dateNaissance, sexe, nationalite, relation, holder.codeFamille(),
        str(societe.get("code")), str(contrat.get("code")), source, str(location.get("code")),
        assurance, covid);
    enqueue(id, matricule, beachFields, "INSERT");
    scheduleOutboxFlushAfterCommit();
    return get(id);
  }

  @Transactional
  public Map<String, Object> update(long id, Map<String, Object> body, Long userId) {
    writeGuard.assertChWriteForbidden();
    Map<String, Object> current = get(id);
    int version = intOr(body.get("version"), intOr(current.get("version"), 1));
    Long societeId = requiredLong(body, "societeId", "La société est obligatoire.");
    Long contratId = requiredLong(body, "contratId", "Le contrat est obligatoire.");
    Long locationId = requiredLong(body, "locationId", "La location est obligatoire.");
    String relation = normalizeRelation(str(body.get("relation")));
    String matricule = str(current.get("matricule"));

    var societe = store.findSocieteDetail(societeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Société introuvable."));
    var contrat = store.findContratDetail(contratId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrat introuvable."));
    var location = store.findLocationDetail(locationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location introuvable."));

    if (str(body.get("medilineIpp")) == null) {
      body = new LinkedHashMap<>(body);
      body.put("medilineIpp", current.get("medilineIpp"));
    }
    HolderResolution holder = resolveHolder(relation, matricule, body);
    String nomComplet = firstNonBlank(str(body.get("nomComplet")), str(current.get("nomComplet")));
    String sexe = normalizeSexe(firstNonBlank(str(body.get("sexe")), str(current.get("sexe"))));
    String nationalite = normalizeNationalite(
        firstNonBlank(str(body.get("nationalite")), str(current.get("nationalite"))));
    LocalDate dateNaissance = parseDate(
        body.get("dateNaissance") != null ? body.get("dateNaissance") : current.get("dateNaissance"));
    String assurance = yn(body.get("assurance"), firstNonBlank(str(current.get("assurance")), "N"));
    String covid = yn(body.get("covid"), firstNonBlank(str(current.get("covid")), "N"));
    String source = firstNonBlank(str(body.get("source")), firstNonBlank(str(current.get("source")), "CM"));

    boolean importOnly = "IMPORT_CLINIQUE".equalsIgnoreCase(str(current.get("sourceSystem")))
        && current.get("legacyKey") != null;
    String syncStatus = importOnly
        ? str(current.get("syncStatus"))
        : (writeGuard.beachEntityWriteEnabled(ENTITY) ? "PENDING" : "SUSPENDED");
    String fingerprint = fingerprint(matricule, nomComplet, societe.get("code"), contrat.get("code"), relation);

    int updated = store.updateAyantErp(
        id,
        nomComplet,
        sexe,
        dateNaissance,
        nationalite,
        relation,
        holder.codeFamille(),
        holder.fkTitulaire(),
        holder.titulaireIpp(),
        societeId,
        str(societe.get("code")),
        contratId,
        str(contrat.get("code")),
        assurance,
        covid,
        locationId,
        str(location.get("code")),
        source,
        syncStatus,
        fingerprint,
        version,
        userId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger la fiche.");
    }
    if (!importOnly) {
      Map<String, Object> beachFields = beachPayload(
          matricule, nomComplet, dateNaissance, sexe, nationalite, relation, holder.codeFamille(),
          str(societe.get("code")), str(contrat.get("code")), source, str(location.get("code")),
          assurance, covid);
      String op = current.get("legacyKey") != null || "SYNCED".equalsIgnoreCase(str(current.get("syncStatus")))
          || "IMPORTED".equalsIgnoreCase(str(current.get("syncStatus")))
          ? "UPDATE"
          : "INSERT";
      enqueue(id, matricule, beachFields, op);
      scheduleOutboxFlushAfterCommit();
    }
    return get(id);
  }

  @Transactional
  public Map<String, Object> setActif(long id, boolean actif, int version, Long userId) {
    get(id);
    int updated = store.setAyantActif(id, actif, version, userId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger la fiche.");
    }
    return get(id);
  }

  private HolderResolution resolveHolder(String relation, String selfMatricule, Map<String, Object> body) {
    if ("A".equals(relation)) {
      return new HolderResolution(selfMatricule, null, str(body.get("medilineIpp")));
    }
    Long fkTitulaire = longOrNull(body.get("titulaireId"));
    String titulaireIpp = str(body.get("titulaireIpp"));
    String codeFamille = str(body.get("codeFamille"));
    String selfIpp = normalizeIpp(str(body.get("medilineIpp")));
    if (fkTitulaire != null) {
      Map<String, Object> tit = store.findAyantDetail(fkTitulaire)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Titulaire introuvable dans les ayants droit."));
      codeFamille = firstNonBlank(str(tit.get("matricule")), codeFamille);
      titulaireIpp = firstNonBlank(str(tit.get("medilineIpp")), titulaireIpp);
    } else if (titulaireIpp != null) {
      var byIpp = store.findAyantByMedilineIpp(titulaireIpp);
      if (byIpp.isPresent()) {
        fkTitulaire = ((Number) byIpp.get().get("id")).longValue();
        codeFamille = firstNonBlank(str(byIpp.get().get("matricule")), codeFamille);
      }
    }
    if (codeFamille == null || codeFamille.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Le titulaire est obligatoire pour ce lien de parenté.");
    }
    String holderIpp = normalizeIpp(titulaireIpp);
    if (selfIpp != null && holderIpp != null && selfIpp.equals(holderIpp)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Le patient ne peut pas être son propre titulaire.");
    }
    if (selfMatricule != null && !selfMatricule.isBlank()
        && codeFamille.trim().equalsIgnoreCase(selfMatricule.trim())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Le patient ne peut pas être son propre titulaire.");
    }
    if (codeFamille.length() > BEACH_MATRICULE_MAX) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
          "Le matricule du titulaire dépasse la limite historique CLINIQUE (10 caractères).");
    }
    return new HolderResolution(codeFamille, fkTitulaire, titulaireIpp);
  }

  private static String normalizeIpp(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return raw.trim().toUpperCase();
  }

  private String resolveBeachMatricule(String medilineIpp, String historiqueHint) {
    if (historiqueHint != null && !historiqueHint.isBlank()) {
      String h = historiqueHint.trim();
      if (h.length() > BEACH_MATRICULE_MAX) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
            "Matricule historique incompatible avec TAYANTDROIT (max 10 caractères).");
      }
      return h;
    }
    String ipp = medilineIpp.trim();
    if (ipp.length() <= BEACH_MATRICULE_MAX) {
      return ipp;
    }
    var existing = store.findAyantByMedilineIpp(ipp);
    if (existing.isPresent()) {
      return str(existing.get().get("matricule"));
    }
    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
        "L'IPP Mediline dépasse 10 caractères et aucune correspondance matricule historique n'est disponible. "
            + "Réplication TAYANTDROIT bloquée — rapprocher d'abord un matricule compatible.");
  }

  private Map<String, Object> beachPayload(
      String matricule,
      String nom,
      LocalDate dateNais,
      String sexe,
      String nationalite,
      String relation,
      String codeFam,
      String codeSoc,
      String contrat,
      String source,
      String location,
      String assurance,
      String covid) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("MATRICULE", matricule);
    fields.put("NOM", truncate(nom, 50));
    fields.put("DATENAIS", dateNais == null ? null : dateNais.toString());
    fields.put("SEXE", sexe);
    fields.put("NATIONALITE", nationalite);
    fields.put("RELATION", relation);
    fields.put("CODEFAM", codeFam);
    fields.put("CODESOC", truncate(codeSoc, 4));
    fields.put("CONTRAT", truncate(contrat, 3));
    fields.put("DATSAIS", LocalDateTime.now().toString());
    fields.put("SOURCE", truncate(source, 3));
    try {
      fields.put("LOCATION", location == null || location.isBlank() ? null : Integer.parseInt(location.trim()));
    } catch (NumberFormatException ex) {
      fields.put("LOCATION", location);
    }
    fields.put("ASSURANCE", assurance);
    fields.put("COVID", covid);
    return fields;
  }

  private void enqueue(long id, String naturalKey, Map<String, Object> fields, String operation) {
    if (referentialRepository.hasBusyOutbox(id, ENTITY)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Une synchronisation est déjà en cours.");
    }
    String payload = writeJson(Map.of(
        "key", naturalKey,
        "fields", fields,
        "operation", operation,
        "entityTable", "erp_ayants_droit"));
    int replaced = referentialRepository.replaceOpenPayload(id, ENTITY, operation, payload, naturalKey);
    if (replaced > 0) {
      return;
    }
    referentialRepository.enqueueOutbox(
        UUID.randomUUID().toString(),
        ENTITY,
        id,
        naturalKey,
        operation,
        payload,
        UUID.randomUUID().toString(),
        writeGuard.beachEntityWriteEnabled(ENTITY) ? "PENDING" : "SUSPENDED");
  }

  private void scheduleOutboxFlushAfterCommit() {
    if (!writeGuard.beachEntityWriteEnabled(ENTITY)) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      flushOutboxQuietly();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        flushOutboxQuietly();
      }
    });
  }

  private int flushOutboxQuietly() {
    ClinicBeachOutboxWorker worker = outboxWorker.getIfAvailable();
    if (worker == null || !writeGuard.beachEntityWriteEnabled(ENTITY)) {
      return 0;
    }
    try {
      return worker.flushPendingBatch(50);
    } catch (Exception ex) {
      log.warn("Flush outbox TAYANTDROIT : {}", ex.getMessage());
      return 0;
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sérialisation impossible.");
    }
  }

  private static String fingerprint(Object... parts) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      for (Object p : parts) {
        if (p != null) {
          md.update(String.valueOf(p).getBytes(StandardCharsets.UTF_8));
          md.update((byte) '|');
        }
      }
      return HexFormat.of().formatHex(md.digest());
    } catch (Exception ex) {
      return UUID.randomUUID().toString().replace("-", "");
    }
  }

  private static String normalizeRelation(String raw) {
    String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    if (!RELATIONS.contains(v)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Lien de parenté non reconnu (A, C, E, P, O).");
    }
    return v;
  }

  private static String normalizeSexe(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String v = raw.trim().toUpperCase(Locale.ROOT);
    if (v.startsWith("M")) {
      return "M";
    }
    if (v.startsWith("F")) {
      return "F";
    }
    return v.substring(0, 1);
  }

  private static String normalizeNationalite(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String v = raw.trim().toUpperCase(Locale.ROOT);
    if (v.startsWith("C") || v.contains("CONGO")) {
      return "C";
    }
    if (v.startsWith("E") || v.contains("ETR") || v.contains("ÉTR")) {
      return "E";
    }
    return v.substring(0, 1);
  }

  private static String yn(Object raw, String defaultValue) {
    if (raw == null) {
      return defaultValue;
    }
    if (raw instanceof Boolean b) {
      return b ? "O" : "N";
    }
    String v = String.valueOf(raw).trim().toUpperCase(Locale.ROOT);
    if (v.equals("1") || v.equals("O") || v.equals("OUI") || v.equals("Y") || v.equals("TRUE")) {
      return "O";
    }
    if (v.equals("0") || v.equals("N") || v.equals("NON") || v.equals("FALSE")) {
      return "N";
    }
    return defaultValue;
  }

  private static String required(Map<String, Object> body, String key, String message) {
    String v = str(body.get(key));
    if (v == null || v.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return v;
  }

  private static Long requiredLong(Map<String, Object> body, String key, String message) {
    Long v = longOrNull(body.get(key));
    if (v == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return v;
  }

  private static String str(Object raw) {
    if (raw == null) {
      return null;
    }
    String t = String.valueOf(raw).trim();
    return t.isEmpty() || "null".equalsIgnoreCase(t) ? null : t;
  }

  private static Long longOrNull(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number n) {
      return n.longValue();
    }
    try {
      String t = String.valueOf(raw).trim();
      return t.isEmpty() ? null : Long.parseLong(t);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static int intOr(Object raw, int fallback) {
    if (raw instanceof Number n) {
      return n.intValue();
    }
    try {
      return raw == null ? fallback : Integer.parseInt(String.valueOf(raw).trim());
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private static LocalDate parseDate(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof LocalDate ld) {
      return ld;
    }
    String t = String.valueOf(raw).trim();
    if (t.length() >= 10) {
      try {
        return LocalDate.parse(t.substring(0, 10));
      } catch (Exception ignored) {
        return null;
      }
    }
    return null;
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a;
    }
    return b;
  }

  private static String truncate(String v, int max) {
    if (v == null) {
      return null;
    }
    return v.length() <= max ? v : v.substring(0, max);
  }

  private record HolderResolution(String codeFamille, Long fkTitulaire, String titulaireIpp) {
  }
}
