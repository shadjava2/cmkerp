package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.LigneTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.LigneTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.TransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Command Service pour la gestion des lignes de transfert interne (écriture uniquement).
 */
@Service
@Transactional
@Slf4j
public class LigneTransfertInterneCommandService {

    private final LigneTransfertInterneRepository ligneTransfertInterneRepository;
    private final TransfertInterneRepository transfertInterneRepository;
    private final LigneTransfertInterneMapper ligneTransfertInterneMapper;
    private final JdbcTemplate jdbcTemplate;

    public LigneTransfertInterneCommandService(
            LigneTransfertInterneRepository ligneTransfertInterneRepository,
            TransfertInterneRepository transfertInterneRepository,
            LigneTransfertInterneMapper ligneTransfertInterneMapper,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.ligneTransfertInterneRepository = ligneTransfertInterneRepository;
        this.transfertInterneRepository = transfertInterneRepository;
        this.ligneTransfertInterneMapper = ligneTransfertInterneMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Crée une nouvelle ligne de transfert interne.
     */
    public LigneTransfertInterneResponse create(Long transfertInterneId, CreateLigneTransfertInterneRequest request, Long currentUserId) {
        log.debug("Création d'une nouvelle ligne de transfert interne pour transfert: {}", transfertInterneId);

        // Vérifier que le transfert interne existe et est modifiable
        TransfertInterne transfert = transfertInterneRepository.findById(transfertInterneId)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", transfertInterneId));

        if (transfert.getStatut() != TransfertInterne.StatutTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible d'ajouter une ligne à un transfert interne validé ou annulé");
        }

        // Vérifier que le stock existe
        if (request.getFkStock() != null) {
            verifyStockExists(request.getFkStock());
            verifyStockNotExpired(request.getFkStock());
            verifyStockAvailable(request.getFkStock(), request.getQuantite(), transfert.getFkPharmacieSource(), transfertInterneId);
        }

        // Créer la ligne
        LigneTransfertInterne ligne = ligneTransfertInterneMapper.toEntity(request, transfertInterneId);
        ligne.setUserCreatedId(currentUserId);
        ligne.setDateCreate(LocalDateTime.now());

        int rows = ligneTransfertInterneRepository.save(ligne);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de la ligne de transfert interne");
        }

        log.info("Ligne de transfert interne créée avec succès: ID: {}", ligne.getId());

        // Récupérer la ligne créée
        LigneTransfertInterne created = ligneTransfertInterneRepository.findById(ligne.getId())
                .orElseThrow(() -> new BusinessException("Ligne créée mais introuvable"));

        String produitNom = getProduitNom(created.getFkStock());
        return ligneTransfertInterneMapper.toResponse(created, produitNom);
    }

    /**
     * Met à jour une ligne de transfert interne existante.
     */
    public LigneTransfertInterneResponse update(Long transfertInterneId, Long ligneId, UpdateLigneTransfertInterneRequest request, Long currentUserId) {
        log.debug("Mise à jour de la ligne de transfert interne ID: {}", ligneId);

        // Vérifier que le transfert interne existe et est modifiable
        TransfertInterne transfert = transfertInterneRepository.findById(transfertInterneId)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", transfertInterneId));

        if (transfert.getStatut() != TransfertInterne.StatutTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible de modifier une ligne d'un transfert interne validé ou annulé");
        }

        LigneTransfertInterne ligne = ligneTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> NotFoundException.entity("LigneTransfertInterne", ligneId));

        // Utiliser le stock de la ligne existante si non fourni dans la requête
        Long stockId = request.getFkStock() != null ? request.getFkStock() : ligne.getFkStock();
        Float quantityToCheck = request.getQuantite() != null ? request.getQuantite() : ligne.getQuantite();

        // Vérifier que le stock existe si fourni
        if (stockId != null) {
            verifyStockExists(stockId);
            verifyStockNotExpired(stockId);
            verifyStockAvailable(stockId, quantityToCheck, transfert.getFkPharmacieSource(), transfertInterneId, ligneId);
        }

        // Mettre à jour la ligne
        ligneTransfertInterneMapper.updateEntityFromRequest(request, ligne);
        ligne.setUserUpdatedId(currentUserId);
        ligne.setDateUpdate(LocalDateTime.now());

        int rows = ligneTransfertInterneRepository.update(ligne);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de la ligne de transfert interne");
        }

        log.info("Ligne de transfert interne mise à jour avec succès: ID: {}", ligneId);

        // Récupérer la ligne mise à jour
        LigneTransfertInterne updated = ligneTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> new BusinessException("Ligne mise à jour mais introuvable"));

        String produitNom = getProduitNom(updated.getFkStock());
        return ligneTransfertInterneMapper.toResponse(updated, produitNom);
    }

    /**
     * Supprime une ligne de transfert interne.
     */
    public void delete(Long transfertInterneId, Long ligneId) {
        log.debug("Suppression de la ligne de transfert interne ID: {}", ligneId);

        // Vérifier que le transfert interne existe et est modifiable
        TransfertInterne transfert = transfertInterneRepository.findById(transfertInterneId)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", transfertInterneId));

        if (transfert.getStatut() != TransfertInterne.StatutTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible de supprimer une ligne d'un transfert interne validé ou annulé");
        }

        // Vérifier que la ligne existe
        ligneTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> NotFoundException.entity("LigneTransfertInterne", ligneId));

        int rows = ligneTransfertInterneRepository.delete(ligneId);
        if (rows == 0) {
            throw new BusinessException("Échec de la suppression de la ligne de transfert interne");
        }

        log.info("Ligne de transfert interne supprimée avec succès: ID: {}", ligneId);
    }

    private void verifyStockExists(Long fkStock) {
        String sql = "SELECT COUNT(*) FROM stock_produits WHERE id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, fkStock);
        if (count == null || count == 0) {
            throw NotFoundException.entity("Stock", fkStock);
        }
    }

    /**
     * Vérifie que le stock n'est pas périmé.
     * Un stock est considéré comme périmé s'il existe une alerte active (notifactif = TRUE)
     * avec une date de péremption <= aujourd'hui.
     */
    private void verifyStockNotExpired(Long fkStock) {
        String sql = """
            SELECT COUNT(*) FROM perimable_alerte_stock
            WHERE fkStock = ? AND notifactif = TRUE AND dateperemtion <= CURDATE()
            """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, fkStock);
        if (count != null && count > 0) {
            throw new BusinessException("Impossible d'ajouter un stock périmé à un transfert interne");
        }
    }

    /**
     * Vérifie que le stock disponible est suffisant pour la quantité demandée.
     */
    private void verifyStockAvailable(Long fkStock, Float quantity, Long fkPharmacieSource, Long fkTransfertInterne) {
        verifyStockAvailable(fkStock, quantity, fkPharmacieSource, fkTransfertInterne, null);
    }

    /**
     * Vérifie que le stock disponible est suffisant pour la quantité demandée.
     */
    private void verifyStockAvailable(Long fkStock, Float quantity, Long fkPharmacieSource, Long fkTransfertInterne, Long ligneId) {
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

        // Récupérer toutes les lignes de transfert interne pour ce stock dans ce transfert (pour calculer la somme totale)
        Float quantiteTotaleAutresLignes = 0f;
        if (fkTransfertInterne != null) {
            String lignesSql = ligneId != null
                ? "SELECT COALESCE(SUM(quantite), 0) FROM lignes_transfert_interne WHERE fkStock = ? AND fkTransfertInterne = ? AND id != ?"
                : "SELECT COALESCE(SUM(quantite), 0) FROM lignes_transfert_interne WHERE fkStock = ? AND fkTransfertInterne = ?";

            try {
                if (ligneId != null) {
                    quantiteTotaleAutresLignes = jdbcTemplate.queryForObject(lignesSql, Float.class, fkStock, fkTransfertInterne, ligneId);
                } else {
                    quantiteTotaleAutresLignes = jdbcTemplate.queryForObject(lignesSql, Float.class, fkStock, fkTransfertInterne);
                }
                if (quantiteTotaleAutresLignes == null) {
                    quantiteTotaleAutresLignes = 0f;
                }
            } catch (Exception e) {
                log.debug("Erreur lors du calcul de la quantité totale des autres lignes: {}", e.getMessage());
                quantiteTotaleAutresLignes = 0f;
            }
        }

        // Calculer la nouvelle quantité totale
        Float nouvelleQuantiteTotale = quantiteTotaleAutresLignes + quantity;

        // Vérifier que la somme totale ne dépasse pas le stock disponible
        if (nouvelleQuantiteTotale > stockDisponible) {
            throw new BusinessException(String.format(
                "La somme totale des quantités (%.2f) dépasse le stock disponible (%.2f). " +
                "Quantité déjà en ligne: %.2f, quantité à %s: %.2f",
                nouvelleQuantiteTotale,
                stockDisponible,
                quantiteTotaleAutresLignes,
                ligneId != null ? "modifier" : "ajouter",
                quantity
            ));
        }

        if (quantity <= 0) {
            throw new BusinessException("La quantité doit être supérieure à 0");
        }
    }

    private String getProduitNom(Long fkStock) {
        if (fkStock == null) {
            return null;
        }
        String sql = "SELECT p.nomcommercial FROM stock_produits sp " +
                "INNER JOIN produits p ON sp.fkProduits = p.id " +
                "WHERE sp.id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, fkStock);
        } catch (Exception e) {
            log.warn("Produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
            return null;
        }
    }
}

