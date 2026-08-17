package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.LigneInventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.mapper.LigneInventaireMapper;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.LigneInventaire;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository.LigneInventaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Service pour la gestion des lignes d'inventaire (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LigneInventaireQueryService {

  private final LigneInventaireRepository ligneInventaireRepository;
  private final LigneInventaireMapper ligneInventaireMapper;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Liste les lignes d'un inventaire en une seule requête SQL.
   *
   * @param operationnel {@code true} → {@code WHERE operationnel = 1} (actifs),
   *                     {@code false} → inactifs uniquement,
   *                     {@code null} → toutes les lignes
   */
  public List<LigneInventaireResponse> findByFkInventaire(Long fkInventaire, Boolean operationnel) {
    StringBuilder sql = new StringBuilder("""
        SELECT
          li.id,
          li.fkInventaire,
          li.fkStock,
          li.quantite_theorique,
          li.quantite_physique,
          li.commentaire,
          li.datecreate,
          li.dateupdate,
          li.usercreateid,
          li.userupdateid,
          p.nomcommercial,
          p.nomscientifique,
          p.codebarre,
          f.designation AS forme,
          d.designation AS dosage,
          c.designation AS conditionnement,
          sp.operationnel AS operationnel
        FROM lignes_inventaire li
        INNER JOIN stock_produits sp ON sp.id = li.fkStock
        INNER JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes f ON p.fkForme = f.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
        WHERE li.fkInventaire = ?
        """);

    List<Object> params = new ArrayList<>();
    params.add(fkInventaire);

    if (Boolean.TRUE.equals(operationnel)) {
      // Actifs uniquement — accélère fortement le comptage
      sql.append(" AND COALESCE(sp.operationnel, 0) = 1 ");
    } else if (Boolean.FALSE.equals(operationnel)) {
      sql.append(" AND COALESCE(sp.operationnel, 0) = 0 ");
    }

    sql.append(" ORDER BY COALESCE(NULLIF(TRIM(p.nomcommercial), ''), p.nomscientifique), li.id ");

    log.debug("Liste lignes inventaire {} operationnel={}", fkInventaire, operationnel);

    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
      Float qteTheo = rs.getObject("quantite_theorique") != null ? rs.getFloat("quantite_theorique") : null;
      Float qtePhys = rs.getObject("quantite_physique") != null ? rs.getFloat("quantite_physique") : null;
      Float ecart = (qteTheo != null && qtePhys != null) ? qtePhys - qteTheo : null;
      String nomcommercial = rs.getString("nomcommercial");
      Timestamp dateCreateTs = rs.getTimestamp("datecreate");
      Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");
      Object op = rs.getObject("operationnel");

      return LigneInventaireResponse.builder()
          .id(rs.getLong("id"))
          .fkInventaire(rs.getLong("fkInventaire"))
          .fkStock(rs.getLong("fkStock"))
          .produitNom(nomcommercial)
          .nomcommercial(nomcommercial)
          .nomscientifique(rs.getString("nomscientifique"))
          .forme(rs.getString("forme"))
          .dosage(rs.getString("dosage"))
          .conditionnement(rs.getString("conditionnement"))
          .peremption(null)
          .codebarre(rs.getString("codebarre"))
          .operationnel(op == null ? null : rs.getBoolean("operationnel"))
          .quantite_theorique(qteTheo)
          .quantite_physique(qtePhys)
          .ecart(ecart)
          .commentaire(rs.getString("commentaire"))
          .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
          .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
          .userCreatedId(rs.getObject("usercreateid", Long.class))
          .userUpdatedId(rs.getObject("userupdateid", Long.class))
          .build();
    }, params.toArray());
  }

  /**
   * Récupère toutes les lignes d'un inventaire (sans filtre opérationnel).
   */
  public List<LigneInventaireResponse> findByFkInventaire(Long fkInventaire) {
    return findByFkInventaire(fkInventaire, null);
  }

  /**
   * Récupère une ligne par son ID avec toutes les informations du produit.
   */
  public LigneInventaireResponse findById(Long id) {
    LigneInventaire ligne = ligneInventaireRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneInventaire", id));

    ProduitInfo produitInfo = getProduitInfo(ligne.getFkStock());
    return ligneInventaireMapper.toResponse(ligne, produitInfo);
  }

  /**
   * Classe publique pour regrouper les informations du produit. Utilisée par le mapper.
   */
  public static class ProduitInfo {
    public String nomcommercial;
    public String nomscientifique;
    public String forme;
    public String dosage;
    public String conditionnement;
    public String peremption;
    public String codebarre;
    public Boolean operationnel;
  }

  private ProduitInfo getProduitInfo(Long fkStock) {
    if (fkStock == null) {
      return new ProduitInfo();
    }

    String sql = """
        SELECT
            p.nomcommercial,
            p.nomscientifique,
            p.codebarre,
            f.designation as forme_designation,
            d.designation as dosage_designation,
            c.designation as conditionnement_designation,
            COALESCE(pa.peremption, NULL) as peremption,
            sp.operationnel AS operationnel
        FROM stock_produits sp
        INNER JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes f ON p.fkForme = f.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = TRUE
            GROUP BY fkStock
        ) pa ON pa.fkStock = sp.id AND sp.qte > 0
        WHERE sp.id = ?
        """;

    try {
      return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
        ProduitInfo info = new ProduitInfo();
        info.nomcommercial = rs.getString("nomcommercial");
        info.nomscientifique = rs.getString("nomscientifique");
        info.codebarre = rs.getString("codebarre");
        info.forme = rs.getString("forme_designation");
        info.dosage = rs.getString("dosage_designation");
        info.conditionnement = rs.getString("conditionnement_designation");
        info.peremption = rs.getString("peremption");
        Object op = rs.getObject("operationnel");
        info.operationnel = op == null ? null : rs.getBoolean("operationnel");
        return info;
      }, fkStock);
    } catch (Exception e) {
      log.warn("Produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
      return new ProduitInfo();
    }
  }

  /**
   * Trouve une ligne d'inventaire par code-barres du produit (recherche dans cet inventaire uniquement).
   */
  public LigneInventaireResponse findByCodebarreInInventaire(Long fkInventaire, String codebarre) {
    if (codebarre == null || codebarre.trim().isEmpty()) {
      throw NotFoundException.entity("LigneInventaire", "codebarre vide");
    }
    String trimmed = codebarre.trim();

    String sql = """
        SELECT li.id
        FROM lignes_inventaire li
        INNER JOIN stock_produits sp ON li.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        WHERE li.fkInventaire = ?
          AND p.codebarre IS NOT NULL
          AND TRIM(p.codebarre) = ?
        LIMIT 1
        """;

    List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), fkInventaire, trimmed);
    if (ids.isEmpty()) {
      throw NotFoundException.entity("LigneInventaire", "codebarre=" + trimmed);
    }
    return findById(ids.get(0));
  }
}
