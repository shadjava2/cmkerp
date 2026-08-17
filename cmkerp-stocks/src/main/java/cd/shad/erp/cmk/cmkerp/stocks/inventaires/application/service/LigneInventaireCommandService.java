package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.InventaireLotsRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.LigneInventaireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.LigneInventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.PerimableAlerteStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.PerimableAlerteService;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.mapper.LigneInventaireMapper;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.LigneInventaire;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository.LigneInventaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des lignes d'inventaire (écriture uniquement). Note: Les lignes
 * sont créées automatiquement par une procédure stockée, on ne peut que les mettre à jour.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LigneInventaireCommandService {

  private final LigneInventaireRepository ligneInventaireRepository;
  private final LigneInventaireMapper ligneInventaireMapper;
  private final JdbcTemplate jdbcTemplate;
  private final PerimableAlerteService perimableAlerteService;

  /**
   * Met à jour une ligne d'inventaire existante. Note: Les lignes sont créées automatiquement par
   * une procédure stockée, on ne peut que les mettre à jour (quantité physique, commentaire).
   */
  public LigneInventaireResponse update(Long id, LigneInventaireRequest request,
      Long currentUserId) {
    log.debug("Mise à jour de la ligne d'inventaire ID: {}", id);

    LigneInventaire ligne = ligneInventaireRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneInventaire", id));

    // Vérifier que l'inventaire parent n'est pas terminé ou annulé
    verifyInventaireCanBeModified(ligne.getFkInventaire());

    // Mettre à jour la ligne
    ligneInventaireMapper.updateEntityFromRequest(request, ligne);
    ligne.setUserUpdatedId(currentUserId);
    ligne.setDateUpdate(LocalDateTime.now());

    int rows = ligneInventaireRepository.update(ligne);
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour de la ligne d'inventaire");
    }

    log.info("Ligne d'inventaire mise à jour avec succès: ID: {}", id);

    // Récupérer la ligne mise à jour
    LigneInventaire updated = ligneInventaireRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Ligne mise à jour mais introuvable"));

    LigneInventaireQueryService.ProduitInfo produitInfo = getProduitInfo(updated.getFkStock());
    return ligneInventaireMapper.toResponse(updated, produitInfo);
  }

  /**
   * Enregistre plusieurs lots (lot + péremption + quantité) et met à jour la quantité physique
   * avec la somme des quantités saisies.
   */
  public LigneInventaireResponse updateWithLots(Long id, InventaireLotsRequest request,
      Long currentUserId) {
    log.debug("Mise à jour inventaire multi-lots pour ligne ID: {}", id);

    LigneInventaire ligne = ligneInventaireRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneInventaire", id));

    verifyInventaireCanBeModified(ligne.getFkInventaire());

    float total = perimableAlerteService.saveInventaireLots(
        ligne.getFkStock(), request.getEntrees(), currentUserId);

    ligne.setQuantite_physique(total);
    ligne.setUserUpdatedId(currentUserId);
    ligne.setDateUpdate(LocalDateTime.now());

    int rows = ligneInventaireRepository.update(ligne);
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour de la ligne d'inventaire");
    }

    log.info("Ligne inventaire {} mise à jour avec total multi-lots: {}", id, total);

    LigneInventaire updated = ligneInventaireRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Ligne mise à jour mais introuvable"));

    LigneInventaireQueryService.ProduitInfo produitInfo = getProduitInfo(updated.getFkStock());
    return ligneInventaireMapper.toResponse(updated, produitInfo);
  }

  public List<PerimableAlerteStockResponse> getLotsForLigne(Long id) {
    LigneInventaire ligne = ligneInventaireRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneInventaire", id));
    return perimableAlerteService.listActiveByStock(ligne.getFkStock());
  }

  private void verifyInventaireCanBeModified(Long fkInventaire) {
    String sql = "SELECT statut FROM inventaires WHERE id = ?";
    try {
      String statut = jdbcTemplate.queryForObject(sql, String.class, fkInventaire);
      if ("TERMINE".equals(statut) || "ANNULE".equals(statut)) {
        throw new BusinessException(
            "Impossible de modifier une ligne d'un inventaire terminé ou annulé");
      }
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw NotFoundException.entity("Inventaire", fkInventaire);
    }
  }

  /**
   * Récupère toutes les informations du produit depuis le stock via JOINs. Respecte la structure du
   * module produits.
   */
  private LigneInventaireQueryService.ProduitInfo getProduitInfo(Long fkStock) {
    if (fkStock == null) {
      return new LigneInventaireQueryService.ProduitInfo();
    }

    String sql = """
        SELECT
            p.nomcommercial,
            p.nomscientifique,
            f.designation as forme_designation,
            d.designation as dosage_designation,
            c.designation as conditionnement_designation,
            COALESCE(pa.peremption, NULL) as peremption
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
        LigneInventaireQueryService.ProduitInfo info =
            new LigneInventaireQueryService.ProduitInfo();
        info.nomcommercial = rs.getString("nomcommercial");
        info.nomscientifique = rs.getString("nomscientifique");
        info.forme = rs.getString("forme_designation");
        info.dosage = rs.getString("dosage_designation");
        info.conditionnement = rs.getString("conditionnement_designation");
        info.peremption = rs.getString("peremption");
        return info;
      }, fkStock);
    } catch (Exception e) {
      log.warn("Produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
      return new LigneInventaireQueryService.ProduitInfo();
    }
  }
}

