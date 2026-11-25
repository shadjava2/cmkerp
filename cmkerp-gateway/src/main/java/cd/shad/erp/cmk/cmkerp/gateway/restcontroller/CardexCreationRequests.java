package cd.shad.erp.cmk.cmkerp.gateway.restcontroller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CardexCreationRequests implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(CardexCreationRequests.class);

  enum Series {
    PATIENT("t_personne", "PP", 5),
    FICHE("t_fiche_medicale", "FM-", 7);

    final String table;
    final String marker;
    final int digits;

    Series(String table, String marker, int digits) {
      this.table = table;
      this.marker = marker;
      this.digits = digits;
    }
  }

  private final NamedParameterJdbcTemplate primary;

  public CardexCreationRequests(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate primary) {
    this.primary = primary;
  }

  @Override
  public void run(ApplicationArguments args) {
    var jdbc = primary.getJdbcTemplate();
    try {
      Boolean exists = jdbc.queryForObject("""
          SELECT EXISTS (SELECT 1 FROM information_schema.tables
                         WHERE table_schema = DATABASE() AND table_name = 'cardex_creation_request')
          """, Boolean.class);
      if (Boolean.TRUE.equals(exists)) return;
      jdbc.execute((ConnectionCallback<Void>) connection -> {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V23__cardex_creation_request.sql"));
        return null;
      });
    } catch (DataAccessException ex) {
      log.warn("Le registre de création Cardex n’a pas pu être initialisé dans la base CMK ERP.");
    }
  }

  String reserve(Series series, UUID id, String actor, MapSqlParameterSource fields) {
    String fingerprint = fingerprint(fields);
    var params = params(series, id).addValue("actor", actor).addValue("hash", fingerprint);
    try {
      primary.update("""
          INSERT INTO cardex_creation_request (series, request_id, actor, payload_hash)
          VALUES (:series, :id, :actor, :hash)
          """, params);
      return null;
    } catch (DuplicateKeyException existing) {
      return primary.queryForObject("""
          SELECT actor, payload_hash, confirmed_code
          FROM cardex_creation_request WHERE series = :series AND request_id = :id
          """, params, (rs, row) -> {
        if (!actor.equals(rs.getString("actor")) || !fingerprint.equals(rs.getString("payload_hash"))) {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet identifiant de création est déjà utilisé.");
        }
        String confirmed = rs.getString("confirmed_code");
        if (confirmed == null) {
          throw new ResponseStatusException(HttpStatus.CONFLICT,
              "Cette création est en cours ou reste à vérifier dans le registre avant un nouvel enregistrement.");
        }
        return confirmed;
      });
    } catch (BadSqlGrammarException unavailable) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Le registre de création Cardex est indisponible dans la base CMK ERP.", unavailable);
    }
  }

  void recordCandidate(Series series, UUID id, String code) {
    if (primary.update("""
        UPDATE cardex_creation_request SET candidate_code = :code
        WHERE series = :series AND request_id = :id AND confirmed_code IS NULL
        """, params(series, id).addValue("code", code)) != 1) {
      throw missingReservation();
    }
  }

  void confirm(Series series, UUID id, String code) {
    if (primary.update("""
        UPDATE cardex_creation_request SET confirmed_code = :code
        WHERE series = :series AND request_id = :id AND candidate_code = :code AND confirmed_code IS NULL
        """, params(series, id).addValue("code", code)) != 1) {
      throw missingReservation();
    }
  }

  void release(Series series, UUID id) {
    primary.update("DELETE FROM cardex_creation_request WHERE series = :series AND request_id = :id AND confirmed_code IS NULL", params(series, id));
  }

  private static MapSqlParameterSource params(Series series, UUID id) {
    return new MapSqlParameterSource("id", id.toString()).addValue("series", series.name());
  }

  private static ResponseStatusException missingReservation() {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
        "La création n’a pas pu être confirmée. Vérifiez le registre avant de réessayer.");
  }

  private static String fingerprint(MapSqlParameterSource fields) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      new TreeMap<>(fields.getValues()).forEach((name, value) -> {
        String text = Objects.toString(value, "");
        digest.update((name + "=" + text.length() + ":" + text).getBytes(StandardCharsets.UTF_8));
      });
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
