package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * Conversion monétaire serveur : ne jamais faire confiance aux totaux client.
 * Stocke prix original + devise + taux ; calcule USD et CDF via echange_devise.
 */
@Service
@Slf4j
public class MoneyConversionService {

  private static final int SCALE = 4;
  private final JdbcTemplate jdbcTemplate;

  public MoneyConversionService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record ConvertedPrice(
      BigDecimal prixOriginal,
      String devise,
      BigDecimal taux,
      Long fkEchangeDevise,
      BigDecimal prixUsd,
      BigDecimal prixCdf) {}

  public record DeviseRate(Long id, String monnaiePrincipale, String monnaieEchange, BigDecimal taux) {}

  /**
   * Convertit un prix unitaire original vers USD et CDF.
   * Règle : taux = unités de monnaieechange pour 1 unité de monnaieprincipale (schéma legacy).
   * Si devise = USD → prixUsd = original ; CDF = original * taux USD→CDF si disponible.
   * Si devise = CDF → prixCdf = original ; USD = original / taux.
   * Sinon utilise fkEchangeDevise ou le taux le plus récent matching la devise.
   */
  public ConvertedPrice convert(BigDecimal prixOriginal, String devise, Long fkEchangeDeviseHint) {
    if (prixOriginal == null) {
      throw new BusinessException("Le prix original est obligatoire");
    }
    if (prixOriginal.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException("Le prix original ne peut pas être négatif");
    }
    String cur = devise != null ? devise.trim().toUpperCase(Locale.ROOT) : "USD";

    DeviseRate rate = resolveRate(cur, fkEchangeDeviseHint);
    BigDecimal prixUsd;
    BigDecimal prixCdf;
    BigDecimal taux = rate != null ? rate.taux() : BigDecimal.ONE;
    Long fk = rate != null ? rate.id() : null;

    if ("USD".equals(cur)) {
      prixUsd = prixOriginal.setScale(SCALE, RoundingMode.HALF_UP);
      prixCdf = taux != null && taux.compareTo(BigDecimal.ZERO) > 0
          ? prixOriginal.multiply(taux).setScale(SCALE, RoundingMode.HALF_UP)
          : null;
    } else if ("CDF".equals(cur) || "FC".equals(cur)) {
      prixCdf = prixOriginal.setScale(SCALE, RoundingMode.HALF_UP);
      if (taux != null && taux.compareTo(BigDecimal.ZERO) > 0) {
        prixUsd = prixOriginal.divide(taux, SCALE, RoundingMode.HALF_UP);
      } else {
        throw new BusinessException("Taux de change introuvable pour convertir CDF → USD");
      }
    } else {
      // Devise tierce : on suppose taux = USD par unité de devise (via echange_devise)
      if (taux == null || taux.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("Taux de change introuvable pour la devise " + cur);
      }
      prixUsd = prixOriginal.multiply(taux).setScale(SCALE, RoundingMode.HALF_UP);
      DeviseRate usdCdf = findUsdCdfRate();
      prixCdf = usdCdf != null && usdCdf.taux() != null
          ? prixUsd.multiply(usdCdf.taux()).setScale(SCALE, RoundingMode.HALF_UP)
          : null;
    }

    return new ConvertedPrice(prixOriginal.setScale(SCALE, RoundingMode.HALF_UP), cur, taux, fk, prixUsd, prixCdf);
  }

  /** Reliquat = max(0, commandée - reçue). */
  public static BigDecimal calculerReliquat(BigDecimal quantiteCommandee, BigDecimal quantiteRecue) {
    BigDecimal cmd = quantiteCommandee != null ? quantiteCommandee : BigDecimal.ZERO;
    BigDecimal rec = quantiteRecue != null ? quantiteRecue : BigDecimal.ZERO;
    return cmd.subtract(rec).max(BigDecimal.ZERO);
  }

  public static boolean isTotalementLivre(BigDecimal quantiteCommandee, BigDecimal quantiteRecue) {
    return calculerReliquat(quantiteCommandee, quantiteRecue).compareTo(BigDecimal.ZERO) == 0
        && quantiteCommandee != null
        && quantiteCommandee.compareTo(BigDecimal.ZERO) > 0;
  }

  public static boolean isPartiellementLivre(BigDecimal quantiteCommandee, BigDecimal quantiteRecue) {
    BigDecimal rec = quantiteRecue != null ? quantiteRecue : BigDecimal.ZERO;
    return rec.compareTo(BigDecimal.ZERO) > 0 && !isTotalementLivre(quantiteCommandee, quantiteRecue);
  }

  private DeviseRate resolveRate(String devise, Long hint) {
    if (hint != null) {
      Optional<DeviseRate> byId = findById(hint);
      if (byId.isPresent()) {
        return byId.get();
      }
    }
    if ("USD".equals(devise)) {
      DeviseRate usdCdf = findUsdCdfRate();
      return usdCdf != null ? usdCdf : new DeviseRate(null, "USD", "CDF", BigDecimal.ONE);
    }
    try {
      return jdbcTemplate.query(
          "SELECT id, monnaieprincipale, monnaieechange, tauxechange FROM echange_devise "
              + "WHERE UPPER(monnaieprincipale) = ? OR UPPER(monnaieechange) = ? "
              + "ORDER BY id DESC LIMIT 1",
          rs -> {
            if (!rs.next()) {
              return null;
            }
            return mapRate(rs);
          },
          devise, devise);
    } catch (Exception e) {
      log.warn("Impossible de résoudre le taux pour {}: {}", devise, e.getMessage());
      return null;
    }
  }

  private Optional<DeviseRate> findById(Long id) {
    try {
      DeviseRate rate = jdbcTemplate.query(
          "SELECT id, monnaieprincipale, monnaieechange, tauxechange FROM echange_devise WHERE id = ?",
          rs -> rs.next() ? mapRate(rs) : null,
          id);
      return Optional.ofNullable(rate);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private DeviseRate findUsdCdfRate() {
    try {
      return jdbcTemplate.query(
          "SELECT id, monnaieprincipale, monnaieechange, tauxechange FROM echange_devise "
              + "WHERE (UPPER(monnaieprincipale)='USD' AND UPPER(monnaieechange) IN ('CDF','FC')) "
              + "OR (UPPER(monnaieprincipale) IN ('CDF','FC') AND UPPER(monnaieechange)='USD') "
              + "ORDER BY id DESC LIMIT 1",
          rs -> rs.next() ? mapRate(rs) : null);
    } catch (Exception e) {
      return null;
    }
  }

  private static DeviseRate mapRate(java.sql.ResultSet rs) throws java.sql.SQLException {
    BigDecimal taux = rs.getBigDecimal("tauxechange");
    if (taux == null) {
      float f = rs.getFloat("tauxechange");
      if (!rs.wasNull()) {
        taux = BigDecimal.valueOf(f);
      }
    }
    return new DeviseRate(
        rs.getLong("id"),
        rs.getString("monnaieprincipale"),
        rs.getString("monnaieechange"),
        taux);
  }
}
