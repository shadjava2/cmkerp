package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.LigneApprovRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.mapper.LigneApprovMapper;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.LigneApprov;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.Approvisionnement;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.ApprovisionnementRepository;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.LigneApprovRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des lignes d'approvisionnement (écriture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LigneApprovCommandService {

  private final LigneApprovRepository ligneApprovRepository;
  private final LigneApprovMapper ligneApprovMapper;
  private final ApprovisionnementRepository approvisionnementRepository;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Crée une nouvelle ligne d'approvisionnement.
   */
  public LigneApprovResponse create(LigneApprovRequest request, Long currentUserId) {
    log.debug("Création d'une nouvelle ligne d'approvisionnement pour approv: {}",
        request.getFkApprov());

    // Vérifier que l'approvisionnement existe
    verifyApprovisionnementExists(request.getFkApprov());
    verifyApprovisionnementEditable(request.getFkApprov());

    // Vérifier que le stock existe si fourni
    if (request.getFkStock() != null) {
      verifyStockExists(request.getFkStock());
    }

    // Créer la ligne
    LigneApprov ligne = ligneApprovMapper.toEntity(request);
    ligne.setUserCreatedId(currentUserId);
    ligne.setDateCreate(LocalDateTime.now());

    int rows = ligneApprovRepository.save(ligne);
    if (rows == 0) {
      throw new BusinessException("Échec de la création de la ligne d'approvisionnement");
    }

    log.info("Ligne d'approvisionnement créée avec succès: ID: {}", ligne.getId());

    // Récupérer la ligne créée
    LigneApprov created = ligneApprovRepository.findById(ligne.getId())
        .orElseThrow(() -> new BusinessException("Ligne créée mais introuvable"));

    String produitNom = getProduitNom(created.getFkStock());
    return ligneApprovMapper.toResponse(created, produitNom);
  }

  /**
   * Met à jour une ligne d'approvisionnement existante.
   */
  public LigneApprovResponse update(Long id, LigneApprovRequest request, Long currentUserId) {
    log.debug("Mise à jour de la ligne d'approvisionnement ID: {}", id);

    LigneApprov ligne = ligneApprovRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneApprov", id));

    verifyApprovisionnementEditable(ligne.getFkApprov());

    // Vérifier que le stock existe si fourni
    if (request.getFkStock() != null) {
      verifyStockExists(request.getFkStock());
    }

    // Mettre à jour la ligne
    ligneApprovMapper.updateEntityFromRequest(request, ligne);
    ligne.setUserUpdatedId(currentUserId);
    ligne.setDateUpdate(LocalDateTime.now());

    int rows = ligneApprovRepository.update(ligne);
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour de la ligne d'approvisionnement");
    }

    log.info("Ligne d'approvisionnement mise à jour avec succès: ID: {}", id);

    // Récupérer la ligne mise à jour
    LigneApprov updated = ligneApprovRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Ligne mise à jour mais introuvable"));

    String produitNom = getProduitNom(updated.getFkStock());
    return ligneApprovMapper.toResponse(updated, produitNom);
  }

  /**
   * Supprime une ligne d'approvisionnement.
   */
  public void delete(Long id) {
    log.debug("Suppression de la ligne d'approvisionnement ID: {}", id);

    // Vérifier que la ligne existe
    verifyLigneExists(id);
    LigneApprov ligne = ligneApprovRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneApprov", id));
    verifyApprovisionnementEditable(ligne.getFkApprov());

    int rows = ligneApprovRepository.deleteById(id);
    if (rows == 0) {
      throw new BusinessException("Échec de la suppression de la ligne d'approvisionnement");
    }

    log.info("Ligne d'approvisionnement supprimée avec succès: ID: {}", id);
  }

  private void verifyApprovisionnementEditable(Long fkApprov) {
    Approvisionnement appro = approvisionnementRepository.findById(fkApprov)
        .orElseThrow(() -> NotFoundException.entity("Approvisionnement", fkApprov));
    if (appro.getStatut() != Approvisionnement.StatutApprovisionnement.EN_ATTENTE) {
      throw new BusinessException(
          "Impossible de modifier les lignes : l'approvisionnement n'est plus en attente");
    }
  }

  private void verifyApprovisionnementExists(Long fkApprov) {
    String sql = "SELECT COUNT(*) FROM approvsionnements WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, fkApprov);
    if (count == null || count == 0) {
      throw NotFoundException.entity("Approvisionnement", fkApprov);
    }
  }

  private void verifyStockExists(Long fkStock) {
    String sql = "SELECT COUNT(*) FROM stock_produits WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, fkStock);
    if (count == null || count == 0) {
      throw NotFoundException.entity("Stock", fkStock);
    }
  }

  private void verifyLigneExists(Long id) {
    ligneApprovRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneApprov", id));
  }

  private String getProduitNom(Long fkStock) {
    if (fkStock == null) {
      return null;
    }
    // Récupérer le nom du produit depuis le stock
    // Note: La colonne est fkProduits (avec 's') et non fkProduit
    String sql = "SELECT p.nomcommercial FROM stock_produits sp "
        + "INNER JOIN produits p ON sp.fkProduits = p.id " + "WHERE sp.id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, String.class, fkStock);
    } catch (Exception e) {
      log.warn("Produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
      return null;
    }
  }
}

