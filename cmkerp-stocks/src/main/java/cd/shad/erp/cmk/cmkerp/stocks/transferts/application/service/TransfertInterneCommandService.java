package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.TransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.LigneTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.TransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.LigneTransfertInterneRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des transferts internes (écriture uniquement).
 */
@Service
@Transactional
@Slf4j
public class TransfertInterneCommandService {

  private static final String ENTITY_NAME = "TransfertInterne";

  private final TransfertInterneRepository transfertInterneRepository;
  private final LigneTransfertInterneRepository ligneTransfertInterneRepository;
  private final TransfertInterneMapper transfertInterneMapper;
  private final LigneTransfertInterneMapper ligneTransfertInterneMapper;
  private final JdbcTemplate jdbcTemplate;

  public TransfertInterneCommandService(
      TransfertInterneRepository transfertInterneRepository,
      LigneTransfertInterneRepository ligneTransfertInterneRepository,
      TransfertInterneMapper transfertInterneMapper,
      LigneTransfertInterneMapper ligneTransfertInterneMapper,
      @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.transfertInterneRepository = transfertInterneRepository;
    this.ligneTransfertInterneRepository = ligneTransfertInterneRepository;
    this.transfertInterneMapper = transfertInterneMapper;
    this.ligneTransfertInterneMapper = ligneTransfertInterneMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Crée un nouveau transfert interne avec ses lignes.
   */
  public TransfertInterneResponse create(CreateTransfertInterneRequest request, Long currentUserId) {
    log.debug("Création d'un nouveau transfert interne - source: {}, destination: {}",
        request.getFkPharmacieSource(), request.getFkPharmacieDestination());

    // Validations
    if (request.getFkPharmacieSource() == null) {
      throw new BusinessException("La pharmacie source est obligatoire");
    }
    if (request.getFkPharmacieDestination() == null) {
      throw new BusinessException("La pharmacie destination est obligatoire");
    }
    if (request.getFkPharmacieSource().equals(request.getFkPharmacieDestination())) {
      throw new BusinessException("La pharmacie source et destination doivent être différentes");
    }

    // Vérifier que les pharmacies existent
    verifyPharmacieExists(request.getFkPharmacieSource());
    verifyPharmacieExists(request.getFkPharmacieDestination());

    // Vérifier que la destination est éligible (service central + même catégorie droit)
    verifyDestinationEligible(request.getFkPharmacieSource(), request.getFkPharmacieDestination());

    // Vérifier les lignes si elles sont fournies (optionnel lors de la création)
    if (request.getLignes() != null && !request.getLignes().isEmpty()) {
      for (CreateLigneTransfertInterneRequest ligne : request.getLignes()) {
        if (ligne.getFkStock() == null) {
          throw new BusinessException("Le stock est obligatoire pour toutes les lignes");
        }
        if (ligne.getQuantite() == null || ligne.getQuantite() <= 0) {
          throw new BusinessException("La quantité doit être positive pour toutes les lignes");
        }
        verifyStockExists(ligne.getFkStock());
        verifyStockAvailable(ligne.getFkStock(), ligne.getQuantite(), request.getFkPharmacieSource());
      }
    }

    // Créer le transfert interne
    TransfertInterne transfert = transfertInterneMapper.toEntity(request);
    transfert.setUserCreatedId(currentUserId);
    transfert.setDateCreate(LocalDateTime.now());

    int rows = transfertInterneRepository.save(transfert);
    if (rows == 0) {
      throw new BusinessException("Échec de la création du transfert interne");
    }

    log.info("Transfert interne créé avec succès: ID: {}", transfert.getId());

    // Créer les lignes si elles sont fournies
    if (request.getLignes() != null && !request.getLignes().isEmpty()) {
      for (CreateLigneTransfertInterneRequest ligneRequest : request.getLignes()) {
        LigneTransfertInterne ligne = ligneTransfertInterneMapper.toEntity(ligneRequest, transfert.getId());
        ligne.setUserCreatedId(currentUserId);
        ligne.setDateCreate(LocalDateTime.now());

        int ligneRows = ligneTransfertInterneRepository.save(ligne);
        if (ligneRows == 0) {
          throw new BusinessException("Échec de la création de la ligne de transfert interne");
        }
      }
      log.info("Transfert interne créé avec {} lignes", request.getLignes().size());
    } else {
      log.info("Transfert interne créé sans lignes. Les lignes pourront être ajoutées ultérieurement.");
    }

    // Récupérer le transfert créé avec les désignations
    TransfertInterne created = transfertInterneRepository.findById(transfert.getId())
        .orElseThrow(() -> new BusinessException("Transfert interne créé mais introuvable"));

    String pharmacieSourceNom = getPharmacieNom(created.getFkPharmacieSource());
    String pharmacieDestinationNom = getPharmacieNom(created.getFkPharmacieDestination());

    return transfertInterneMapper.toResponse(created, pharmacieSourceNom, pharmacieDestinationNom);
  }

  /**
   * Valide un transfert interne (passe le statut à TRANSFEREE et déduit le stock source).
   */
  public void valider(Long id, Long currentUserId) {
    log.debug("Validation du transfert interne ID: {}", id);

    TransfertInterne transfert = transfertInterneRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));

    List<LigneTransfertInterne> lignes = ligneTransfertInterneRepository.findByFkTransfertInterne(id);
    boolean hasValidLigne = lignes.stream()
        .anyMatch(l -> l.getQuantite() != null && l.getQuantite() > 0);
    if (!hasValidLigne) {
      throw new BusinessException(
          "Impossible de valider: au moins une ligne avec une quantité > 0 est obligatoire");
    }

    updateStocksOnValidation(transfert, lignes, currentUserId);

    transfert.valider(currentUserId);

    int rows = transfertInterneRepository.update(transfert);
    if (rows == 0) {
      throw new BusinessException("Échec de la validation du transfert interne");
    }

    log.info("Transfert interne validé avec succès: ID: {}", id);
  }

  /**
   * Déduit les quantités transférées du stock de la pharmacie source.
   */
  private void updateStocksOnValidation(TransfertInterne transfert, List<LigneTransfertInterne> lignes,
      Long currentUserId) {
    Long fkPharmacieSource = transfert.getFkPharmacieSource();

    for (LigneTransfertInterne ligne : lignes) {
      if (ligne.getQuantite() == null || ligne.getQuantite() <= 0) {
        continue;
      }

      verifyStockAvailable(ligne.getFkStock(), ligne.getQuantite(), fkPharmacieSource);

      String reduceStockSql = """
          UPDATE stock_produits
          SET qte = qte - ?,
              dateupdate = CURRENT_TIMESTAMP,
              userupdateid = ?
          WHERE id = ? AND fkPharmacies = ? AND qte >= ?
          """;
      int rowsReduced = jdbcTemplate.update(reduceStockSql,
          ligne.getQuantite(),
          currentUserId,
          ligne.getFkStock(),
          fkPharmacieSource,
          ligne.getQuantite());

      if (rowsReduced == 0) {
        throw new BusinessException(String.format(
            "Impossible de déduire le stock source (stock ID: %d, quantité: %.2f)",
            ligne.getFkStock(), ligne.getQuantite()));
      }
      log.debug("Stock source déduit à la validation - stock ID: {}, quantité: {}",
          ligne.getFkStock(), ligne.getQuantite());
    }
  }

  /**
   * Annule un transfert interne (passe le statut à ANNULEE).
   */
  public void annuler(Long id, Long currentUserId) {
    log.debug("Annulation du transfert interne ID: {}", id);

    TransfertInterne transfert = transfertInterneRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));

    transfert.annuler(currentUserId);

    int rows = transfertInterneRepository.update(transfert);
    if (rows == 0) {
      throw new BusinessException("Échec de l'annulation du transfert interne");
    }

    log.info("Transfert interne annulé avec succès: ID: {}", id);
  }

  private void verifyPharmacieExists(Long fkPharmacie) {
    String sql = "SELECT COUNT(*) FROM pharmacies WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, fkPharmacie);
    if (count == null || count == 0) {
      throw NotFoundException.entity("Pharmacie", fkPharmacie);
    }
  }

  /**
   * Vérifie les droits catégories pour le transfert.
   * <p>La destination doit avoir au moins une catégorie dans {@code droits_categorie}.
   * Si la source n'a pas de droits catégories (ex. service d'hospitalisation), le transfert
   * reste autorisé tant que la destination en possède (ex. pharmacie centrale).
   */
  private void verifyDestinationEligible(Long sourcePharmacieId, Long destinationPharmacieId) {
    String categoriesSql = "SELECT DISTINCT fkCategorie FROM droits_categorie WHERE fkPharmacie = ?";

    List<Long> destCategories = jdbcTemplate.queryForList(categoriesSql, Long.class, destinationPharmacieId);
    if (destCategories.isEmpty()) {
      throw new BusinessException("La pharmacie destination n'a aucune catégorie de produits");
    }

    List<Long> sourceCategories = jdbcTemplate.queryForList(categoriesSql, Long.class, sourcePharmacieId);
    if (sourceCategories.isEmpty()) {
      return;
    }

    String placeholders = sourceCategories.stream()
        .map(c -> "?")
        .collect(java.util.stream.Collectors.joining(","));
    String commonCategoriesSql = "SELECT COUNT(DISTINCT fkCategorie) FROM droits_categorie "
        + "WHERE fkPharmacie = ? AND fkCategorie IN (" + placeholders + ")";

    List<Object> params = new ArrayList<>();
    params.add(destinationPharmacieId);
    params.addAll(sourceCategories);

    Long commonCount = jdbcTemplate.queryForObject(commonCategoriesSql, Long.class, params.toArray());
    if (commonCount == null || commonCount == 0) {
      throw new BusinessException(
          "La pharmacie destination n'a aucune catégorie de produits commune avec la source");
    }
  }

  private void verifyStockExists(Long fkStock) {
    String sql = "SELECT COUNT(*) FROM stock_produits WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, fkStock);
    if (count == null || count == 0) {
      throw NotFoundException.entity("Stock", fkStock);
    }
  }

  /**
   * Vérifie que le stock disponible est suffisant pour la quantité demandée.
   */
  private void verifyStockAvailable(Long fkStock, Float quantity, Long fkPharmacieSource) {
    if (fkStock == null || quantity == null) {
      return;
    }

    // Récupérer le stock disponible pour la pharmacie source
    String stockSql = "SELECT qte FROM stock_produits WHERE id = ? AND fkPharmacies = ?";
    Float stockDisponible;
    try {
      stockDisponible = jdbcTemplate.queryForObject(stockSql, Float.class, fkStock, fkPharmacieSource);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération du stock pour ID: {} - Erreur: {}", fkStock, e.getMessage());
      throw new BusinessException("Impossible de récupérer le stock disponible");
    }

    if (stockDisponible == null) {
      throw new BusinessException("Stock introuvable pour le produit dans la pharmacie source");
    }

    if (stockDisponible < 0) {
      throw new BusinessException(String.format("Le stock ne peut pas être négatif. Stock actuel: %.2f", stockDisponible));
    }

    if (quantity > stockDisponible) {
      throw new BusinessException(String.format(
          "La quantité demandée (%.2f) dépasse le stock disponible (%.2f)",
          quantity, stockDisponible
      ));
    }

    if (quantity <= 0) {
      throw new BusinessException("La quantité doit être supérieure à 0");
    }
  }

  private String getPharmacieNom(Long fkPharmacie) {
    if (fkPharmacie == null) {
      return null;
    }
    String sql = "SELECT designation FROM pharmacies WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, String.class, fkPharmacie);
    } catch (Exception e) {
      log.warn("Pharmacie non trouvée pour ID: {}", fkPharmacie);
      return null;
    }
  }
}

