package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniquePrixInfo;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.PrixAjustementApplyRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.PrixAjustementApplyResult;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.PrixEcartPageResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Comparaison / alignement prixachat ERP ↔ PAU CLINIQUE (catégories pharmacie 2 et 3).
 */
@Service
@Slf4j
public class PrixAjustementService {

  private static final String ERP_PHARMACY_CATEGORIES = "p.fkCategorie IN (2, 3)";
  private static final BigDecimal EPS = new BigDecimal("0.005");

  private final JdbcTemplate jdbc;
  private final CliniqueLookupRepository cliniqueLookupRepository;

  public PrixAjustementService(
      @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc,
      CliniqueLookupRepository cliniqueLookupRepository) {
    this.jdbc = jdbc;
    this.cliniqueLookupRepository = cliniqueLookupRepository;
  }

  @Transactional(readOnly = true)
  public PrixEcartPageResponse listEcarts(String query) {
    List<PrixEcartPageResponse.PrixEcartItemResponse> linked = loadLinkedErp(query);
    Map<String, CliniquePrixInfo> byCode = new HashMap<>();
    if (cliniqueLookupRepository.isAvailable() && !linked.isEmpty()) {
      List<String> codes = linked.stream()
          .map(PrixEcartPageResponse.PrixEcartItemResponse::getCodeClinique)
          .distinct()
          .toList();
      for (CliniquePrixInfo info : cliniqueLookupRepository.findPrixByCodes(codes)) {
        if (info.code() != null) {
          byCode.put(info.code().trim(), info);
        }
      }
    }

    List<PrixEcartPageResponse.PrixEcartItemResponse> above = new ArrayList<>();
    List<PrixEcartPageResponse.PrixEcartItemResponse> below = new ArrayList<>();
    long equal = 0;
    for (PrixEcartPageResponse.PrixEcartItemResponse item : linked) {
      CliniquePrixInfo cli = byCode.get(item.getCodeClinique());
      if (cli != null) {
        item.setDesignationClinique(cli.designation());
        item.setPau(cli.pau());
        item.setStockClinique(cli.stock());
      }
      BigDecimal erp = item.getPrixachat() == null ? BigDecimal.ZERO : item.getPrixachat();
      BigDecimal pau = item.getPau() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getPau());
      BigDecimal ecart = erp.subtract(pau).setScale(4, RoundingMode.HALF_UP);
      item.setEcart(ecart);
      if (ecart.abs().compareTo(EPS) <= 0) {
        equal++;
      } else if (ecart.signum() > 0) {
        above.add(item);
      } else {
        below.add(item);
      }
    }

    return PrixEcartPageResponse.builder()
        .erpAboveCount(above.size())
        .erpBelowCount(below.size())
        .equalCount(equal)
        .cliniqueAvailable(cliniqueLookupRepository.isAvailable())
        .erpAbove(above)
        .erpBelow(below)
        .build();
  }

  @Transactional
  public PrixAjustementApplyResult apply(PrixAjustementApplyRequest request, Long userId) {
    List<String> failures = new ArrayList<>();
    int success = 0;
    int skipped = 0;
    List<Long> ids = request.getProduitIds() == null ? List.of() : request.getProduitIds();
    long uid = userId != null ? userId : 0L;

    for (Long produitId : ids) {
      try {
        Map<String, Object> row = loadOne(produitId);
        if (row == null) {
          failures.add("#" + produitId + " introuvable ou hors pharmacie");
          continue;
        }
        String code = trim((String) row.get("code"));
        BigDecimal prixachat = (BigDecimal) row.get("prixachat");
        if (code == null || code.isBlank()) {
          failures.add("#" + produitId + " sans CODECLINIQUE");
          continue;
        }
        if (request.getDirection() == PrixAjustementApplyRequest.Direction.ERP_TO_CLINIQUE) {
          if (prixachat == null) {
            skipped++;
            continue;
          }
          boolean ok = cliniqueLookupRepository.updatePau(code, prixachat.doubleValue());
          if (!ok) {
            failures.add("#" + produitId + " PAU CLINIQUE non écrit (" + code + ")");
            continue;
          }
          success++;
        } else {
          List<CliniquePrixInfo> infos = cliniqueLookupRepository.findPrixByCodes(List.of(code));
          if (infos.isEmpty() || infos.get(0).pau() == null) {
            failures.add("#" + produitId + " PAU CLINIQUE absent (" + code + ")");
            continue;
          }
          BigDecimal pau = BigDecimal.valueOf(infos.get(0).pau()).setScale(4, RoundingMode.HALF_UP);
          jdbc.update(
              "UPDATE produits SET prixachat = ?, dateupdate = NOW(), userupdateid = ? WHERE id = ?",
              pau,
              uid,
              produitId);
          success++;
        }
      } catch (Exception ex) {
        failures.add("#" + produitId + " " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
      }
    }

    return PrixAjustementApplyResult.builder()
        .requested(ids.size())
        .successCount(success)
        .skippedCount(skipped)
        .failureCount(failures.size())
        .failures(failures)
        .build();
  }

  /**
   * Applique la règle prix à la validation d'un approvisionnement (ERP + PAU CLINIQUE).
   * Ne fait pas échouer le bon si CLINIQUE est indisponible.
   */
  public void syncOnApprovisionnement(Long approvId, Long userId) {
    if (approvId == null) {
      return;
    }
    long uid = userId != null ? userId : 0L;
    List<Map<String, Object>> lignes;
    try {
      lignes = jdbc.queryForList(
          """
          SELECT p.id AS produitId, p.prixachat AS prixErp, TRIM(p.CODECLINIQUE) AS codeClinique,
                 COALESCE(sp.qte, 0) AS stockErp, la.prixachat AS prixLigne, COALESCE(la.qt, 0) AS qt
          FROM lignes_approv la
          INNER JOIN stock_produits sp ON sp.id = la.fkStock
          INNER JOIN produits p ON p.id = sp.fkProduits
          WHERE la.fkApprov = ?
            AND p.fkCategorie IN (2, 3)
          """,
          approvId);
    } catch (DataAccessException ex) {
      log.warn("syncOnApprovisionnement lecture lignes: {}", ex.getMessage());
      return;
    }

    Map<Long, BigDecimal> runningErp = new HashMap<>();
    Map<Long, Double> runningStock = new HashMap<>();

    for (Map<String, Object> ligne : lignes) {
      Long produitId = toLong(ligne.get("produitId"));
      if (produitId == null) {
        continue;
      }
      BigDecimal current = runningErp.containsKey(produitId)
          ? runningErp.get(produitId)
          : toBigDecimal(ligne.get("prixErp"));
      double stock = runningStock.containsKey(produitId)
          ? runningStock.get(produitId)
          : toDouble(ligne.get("stockErp"));
      BigDecimal incoming = toBigDecimal(ligne.get("prixLigne"));
      BigDecimal resolved = PrixAchatPolicy.resolve(current, incoming, stock);
      if (resolved != null && (current == null || resolved.compareTo(current) != 0)) {
        try {
          jdbc.update(
              "UPDATE produits SET prixachat = ?, dateupdate = NOW(), userupdateid = ? WHERE id = ?",
              resolved,
              uid,
              produitId);
        } catch (DataAccessException ex) {
          log.warn("syncOnApprovisionnement ERP produit {}: {}", produitId, ex.getMessage());
        }
      }
      runningErp.put(produitId, resolved != null ? resolved : current);
      runningStock.put(produitId, stock + toDouble(ligne.get("qt")));

      String code = trim((String) ligne.get("codeClinique"));
      if (code == null || code.isBlank() || !cliniqueLookupRepository.isAvailable()) {
        continue;
      }
      try {
        List<CliniquePrixInfo> infos = cliniqueLookupRepository.findPrixByCodes(List.of(code));
        Double pau = infos.isEmpty() ? null : infos.get(0).pau();
        double stockCli = infos.isEmpty() || infos.get(0).stock() == null ? 0d : infos.get(0).stock();
        BigDecimal currentPau = pau == null ? null : BigDecimal.valueOf(pau);
        BigDecimal newPau = PrixAchatPolicy.resolve(currentPau, resolved != null ? resolved : incoming, stockCli);
        if (newPau != null) {
          cliniqueLookupRepository.updatePau(code, newPau.doubleValue());
        }
      } catch (Exception ex) {
        log.warn("syncOnApprovisionnement PAU {} : {}", code, ex.getMessage());
      }
    }
  }

  private List<PrixEcartPageResponse.PrixEcartItemResponse> loadLinkedErp(String query) {
    StringBuilder sql = new StringBuilder(
        """
        SELECT p.id, p.nomcommercial, p.nomscientifique, p.prixachat, TRIM(p.CODECLINIQUE) AS codeClinique,
               f.designation AS forme, d.designation AS dosage, c.designation AS conditionnement,
               ct.designation AS categorie,
               COALESCE((SELECT SUM(sp.qte) FROM stock_produits sp WHERE sp.fkProduits = p.id), 0) AS stockErp
        FROM produits p
        LEFT JOIN formes f ON p.fkForme = f.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
        LEFT JOIN categorie_produit ct ON p.fkCategorie = ct.id
        WHERE """
            + ERP_PHARMACY_CATEGORIES
            + " AND p.CODECLINIQUE IS NOT NULL AND TRIM(p.CODECLINIQUE) <> ''");
    List<Object> args = new ArrayList<>();
    if (query != null && !query.isBlank()) {
      sql.append(
          " AND (LOWER(IFNULL(p.nomcommercial,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.nomscientifique,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.CODECLINIQUE,'')) LIKE ?)");
      String like = "%" + query.trim().toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    sql.append(" ORDER BY p.nomcommercial ASC LIMIT 5000");
    try {
      return jdbc.query(sql.toString(), this::mapItem, args.toArray());
    } catch (DataAccessException ex) {
      throw new BusinessException("Erreur lecture écarts prix: " + ex.getMostSpecificCause().getMessage());
    }
  }

  private PrixEcartPageResponse.PrixEcartItemResponse mapItem(ResultSet rs, int i) throws SQLException {
    return PrixEcartPageResponse.PrixEcartItemResponse.builder()
        .produitId(rs.getLong("id"))
        .nomcommercial(rs.getString("nomcommercial"))
        .nomscientifique(rs.getString("nomscientifique"))
        .forme(rs.getString("forme"))
        .dosage(rs.getString("dosage"))
        .conditionnement(rs.getString("conditionnement"))
        .categorie(rs.getString("categorie"))
        .prixachat(rs.getBigDecimal("prixachat"))
        .codeClinique(trim(rs.getString("codeClinique")))
        .stockErp(rs.getDouble("stockErp"))
        .build();
  }

  private Map<String, Object> loadOne(Long produitId) {
    List<Map<String, Object>> rows = jdbc.query(
        "SELECT p.id, p.prixachat, TRIM(p.CODECLINIQUE) AS code FROM produits p WHERE p.id = ? AND "
            + ERP_PHARMACY_CATEGORIES,
        (rs, i) -> {
          Map<String, Object> m = new HashMap<>();
          m.put("id", rs.getLong("id"));
          m.put("prixachat", rs.getBigDecimal("prixachat"));
          m.put("code", rs.getString("code"));
          return m;
        },
        produitId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  private static String trim(String v) {
    return v == null ? null : v.trim();
  }

  private static Long toLong(Object v) {
    if (v instanceof Number n) {
      return n.longValue();
    }
    return null;
  }

  private static double toDouble(Object v) {
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    return 0d;
  }

  private static BigDecimal toBigDecimal(Object v) {
    if (v instanceof BigDecimal bd) {
      return bd;
    }
    if (v instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    return null;
  }
}
