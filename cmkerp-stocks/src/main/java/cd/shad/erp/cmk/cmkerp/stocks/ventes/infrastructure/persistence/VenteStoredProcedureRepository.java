package cd.shad.erp.cmk.cmkerp.stocks.ventes.infrastructure.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'appel des stored procedures de validation des ventes.
 *
 * <p>
 * SP_VALIDATE_VENTE : compatibilité double schéma MySQL (3 IN seulement en prod legacy,
 * ou 3 IN + 2 OUT après migration V5).
 */
@Repository
@Slf4j
public class VenteStoredProcedureRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ValidateVenteStoredProcedure validateVenteSpWithOut;
  private final AnnulerVenteStoredProcedure annulerVenteSp;
  private final AnnulerVenteRembourseStoredProcedure annulerVenteRembourseSp;

  public VenteStoredProcedureRepository(
      DataSource dataSource, @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.validateVenteSpWithOut = new ValidateVenteStoredProcedure(dataSource);
    this.annulerVenteSp = new AnnulerVenteStoredProcedure(dataSource);
    this.annulerVenteRembourseSp = new AnnulerVenteRembourseStoredProcedure(dataSource);
  }

  /**
   * Résultat de l'exécution d'une stored procedure.
   */
  public static class SpResult {
    private final int resultCode;
    private final String resultMessage;

    public SpResult(int resultCode, String resultMessage) {
      this.resultCode = resultCode;
      this.resultMessage = resultMessage;
    }

    public int getResultCode() {
      return resultCode;
    }

    public String getResultMessage() {
      return resultMessage;
    }

    public boolean isSuccess() {
      return resultCode == 0;
    }
  }

  /**
   * Valide une vente via stored procedure.
   */
  public SpResult validateVente(Long venteId, String statut, Long userId) {
    try {
      return callValidateVenteWithOutParams(venteId, statut, userId);
    } catch (DataAccessException ex) {
      if (isOutParameterMismatch(ex)) {
        log.warn(
            "SP_VALIDATE_VENTE sans paramètres OUT détectée — appel legacy 3 paramètres IN (vente={})",
            venteId);
        return callValidateVenteInOnly(venteId, statut, userId);
      }
      throw ex;
    }
  }

  private SpResult callValidateVenteWithOutParams(Long venteId, String statut, Long userId) {
    Map<String, Object> params = new HashMap<>();
    params.put("p_vente_id", venteId);
    params.put("p_statut", statut);
    params.put("p_user_id", userId);

    Map<String, Object> result = validateVenteSpWithOut.execute(params);

    int resultCode = (Integer) result.get("p_result_code");
    String resultMessage = (String) result.get("p_result_message");

    return new SpResult(resultCode, resultMessage);
  }

  private SpResult callValidateVenteInOnly(Long venteId, String statut, Long userId) {
    jdbcTemplate.execute((Connection connection) -> {
      try (CallableStatement cs =
          connection.prepareCall("{call SP_VALIDATE_VENTE(?, ?, ?)}")) {
        cs.setLong(1, venteId);
        cs.setString(2, statut);
        cs.setLong(3, userId);
        cs.execute();
      }
      return null;
    });
    return new SpResult(0, "Validation réussie");
  }

  private static boolean isOutParameterMismatch(Throwable ex) {
    Throwable current = ex;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && message.contains("not an OUT parameter")) {
        return true;
      }
      if (current instanceof SQLException sqlEx) {
        String sqlState = sqlEx.getSQLState();
        if ("HY000".equals(sqlState) && message != null && message.contains("OUT parameter")) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  /**
   * Annule une vente via stored procedure.
   */
  public SpResult annulerVente(Long venteId, Long userId) {
    try {
      Map<String, Object> params = new HashMap<>();
      params.put("p_vente_id", venteId);
      params.put("p_user_id", userId);

      Map<String, Object> result = annulerVenteSp.execute(params);

      int resultCode = (Integer) result.get("p_result_code");
      String resultMessage = (String) result.get("p_result_message");

      return new SpResult(resultCode, resultMessage);
    } catch (DataAccessException ex) {
      if (isOutParameterMismatch(ex)) {
        log.warn(
            "SP_ANNULER_VENTE sans paramètres OUT détectée — appel legacy 2 paramètres IN (vente={})",
            venteId);
        return callAnnulerVenteInOnly(venteId, userId);
      }
      throw ex;
    }
  }

  private SpResult callAnnulerVenteInOnly(Long venteId, Long userId) {
    jdbcTemplate.execute((Connection connection) -> {
      try (CallableStatement cs =
          connection.prepareCall("{call SP_ANNULER_VENTE(?, ?)}")) {
        cs.setLong(1, venteId);
        cs.setLong(2, userId);
        cs.execute();
      }
      return null;
    });
    return new SpResult(0, "Annulation réussie");
  }

  /**
   * Annule une vente avec remboursement via stored procedure.
   */
  public SpResult annulerVenteRembourse(Long venteId, Long userId) {
    try {
      Map<String, Object> params = new HashMap<>();
      params.put("p_vente_id", venteId);
      params.put("p_user_id", userId);

      Map<String, Object> result = annulerVenteRembourseSp.execute(params);

      int resultCode = (Integer) result.get("p_result_code");
      String resultMessage = (String) result.get("p_result_message");

      return new SpResult(resultCode, resultMessage);
    } catch (DataAccessException ex) {
      if (isOutParameterMismatch(ex)) {
        log.warn(
            "SP_ANNULER_VENTE_REMBOURSE sans paramètres OUT détectée — appel legacy 2 paramètres IN (vente={})",
            venteId);
        return callAnnulerVenteRembourseInOnly(venteId, userId);
      }
      throw ex;
    }
  }

  private SpResult callAnnulerVenteRembourseInOnly(Long venteId, Long userId) {
    jdbcTemplate.execute((Connection connection) -> {
      try (CallableStatement cs =
          connection.prepareCall("{call SP_ANNULER_VENTE_REMBOURSE(?, ?)}")) {
        cs.setLong(1, venteId);
        cs.setLong(2, userId);
        cs.execute();
      }
      return null;
    });
    return new SpResult(0, "Annulation avec remboursement réussie");
  }

  /**
   * Stored procedure pour valider une vente (schéma V5 avec OUT).
   */
  private static class ValidateVenteStoredProcedure extends StoredProcedure {
    public ValidateVenteStoredProcedure(DataSource dataSource) {
      super(dataSource, "SP_VALIDATE_VENTE");
      declareParameter(new SqlParameter("p_vente_id", Types.BIGINT));
      declareParameter(new SqlParameter("p_statut", Types.VARCHAR));
      declareParameter(new SqlParameter("p_user_id", Types.BIGINT));
      declareParameter(new SqlOutParameter("p_result_code", Types.INTEGER));
      declareParameter(new SqlOutParameter("p_result_message", Types.VARCHAR));
      compile();
    }
  }

  /**
   * Stored procedure pour annuler une vente.
   */
  private static class AnnulerVenteStoredProcedure extends StoredProcedure {
    public AnnulerVenteStoredProcedure(DataSource dataSource) {
      super(dataSource, "SP_ANNULER_VENTE");
      declareParameter(new SqlParameter("p_vente_id", Types.BIGINT));
      declareParameter(new SqlParameter("p_user_id", Types.BIGINT));
      declareParameter(new SqlOutParameter("p_result_code", Types.INTEGER));
      declareParameter(new SqlOutParameter("p_result_message", Types.VARCHAR));
      compile();
    }
  }

  /**
   * Stored procedure pour annuler une vente avec remboursement.
   */
  private static class AnnulerVenteRembourseStoredProcedure extends StoredProcedure {
    public AnnulerVenteRembourseStoredProcedure(DataSource dataSource) {
      super(dataSource, "SP_ANNULER_VENTE_REMBOURSE");
      declareParameter(new SqlParameter("p_vente_id", Types.BIGINT));
      declareParameter(new SqlParameter("p_user_id", Types.BIGINT));
      declareParameter(new SqlOutParameter("p_result_code", Types.INTEGER));
      declareParameter(new SqlOutParameter("p_result_message", Types.VARCHAR));
      compile();
    }
  }
}
