package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.LigneVenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.LigneVenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper.LigneVenteMapper;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.LigneVente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.LigneVenteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Command Service pour la gestion des lignes de vente (écriture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LigneVenteCommandService {

    private final LigneVenteRepository ligneVenteRepository;
    private final LigneVenteMapper ligneVenteMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Crée une nouvelle ligne de vente.
     */
    public LigneVenteResponse create(LigneVenteRequest request, Long currentUserId) {
        log.debug("Création d'une nouvelle ligne de vente pour vente: {}", request.getFkVente());

        // Vérifier que la vente existe
        verifyVenteExists(request.getFkVente());

        // Vérifier que le stock existe si fourni
        if (request.getFkStock() != null) {
            verifyStockExists(request.getFkStock());

            // Vérifier le stock disponible et la quantité
            if (request.getQt() != null && request.getQt() > 0) {
                verifyStockAvailable(request.getFkStock(), request.getQt(), request.getFkVente());
            }
        }

        // Créer la ligne
        LigneVente ligne = ligneVenteMapper.toEntity(request);
        ligne.setUserCreatedId(currentUserId);
        ligne.setDateCreate(LocalDateTime.now());

        int rows = ligneVenteRepository.save(ligne);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de la ligne de vente");
        }

        log.info("Ligne de vente créée avec succès: ID: {}", ligne.getId());

        // Récupérer la ligne créée
        LigneVente created = ligneVenteRepository.findById(ligne.getId())
                .orElseThrow(() -> new BusinessException("Ligne créée mais introuvable"));

        StockProduitInfo info = getStockProduitInfo(created.getFkStock());
        return ligneVenteMapper.toResponse(
                created,
                info != null ? info.produitNom() : null,
                info != null ? info.stockActuel() : null);
    }

    /**
     * Met à jour une ligne de vente existante.
     */
    public LigneVenteResponse update(Long id, LigneVenteRequest request, Long currentUserId) {
        log.debug("Mise à jour de la ligne de vente ID: {}", id);

        LigneVente ligne = ligneVenteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("LigneVente", id));

        // Utiliser le stock de la ligne existante si non fourni dans la requête
        Long stockId = request.getFkStock() != null ? request.getFkStock() : ligne.getFkStock();

        // Vérifier que le stock existe si fourni
        if (stockId != null) {
            verifyStockExists(stockId);

            // Vérifier le stock disponible et la quantité
            Float quantityToCheck = request.getQt() != null ? request.getQt() : ligne.getQt();
            if (quantityToCheck != null && quantityToCheck > 0) {
                // Si on met à jour la quantité, vérifier le nouveau stock disponible
                // Sinon, vérifier avec la quantité existante
                verifyStockAvailable(stockId, quantityToCheck, ligne.getFkVente(), id);
            }
        }

        // Mettre à jour la ligne
        ligneVenteMapper.updateEntityFromRequest(request, ligne);
        ligne.setUserUpdatedId(currentUserId);
        ligne.setDateUpdate(LocalDateTime.now());

        int rows = ligneVenteRepository.update(ligne);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de la ligne de vente");
        }

        log.info("Ligne de vente mise à jour avec succès: ID: {}", id);

        // Récupérer la ligne mise à jour
        LigneVente updated = ligneVenteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ligne mise à jour mais introuvable"));

        StockProduitInfo info = getStockProduitInfo(updated.getFkStock());
        return ligneVenteMapper.toResponse(
                updated,
                info != null ? info.produitNom() : null,
                info != null ? info.stockActuel() : null);
    }

    /**
     * Supprime une ligne de vente.
     */
    public void delete(Long id) {
        log.debug("Suppression de la ligne de vente ID: {}", id);

        // Vérifier que la ligne existe
        ligneVenteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("LigneVente", id));

        int rows = ligneVenteRepository.delete(id);
        if (rows == 0) {
            throw new BusinessException("Échec de la suppression de la ligne de vente");
        }

        log.info("Ligne de vente supprimée avec succès: ID: {}", id);
    }

    private void verifyVenteExists(Long fkVente) {
        String sql = "SELECT COUNT(*) FROM ventes WHERE id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, fkVente);
        if (count == null || count == 0) {
            throw NotFoundException.entity("Vente", fkVente);
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
     * Pour les ventes, on vérifie que le stock n'est pas négatif et que la quantité ne dépasse pas le stock disponible.
     */
    private void verifyStockAvailable(Long fkStock, Float quantity, Long fkVente) {
        verifyStockAvailable(fkStock, quantity, fkVente, null);
    }

    /**
     * Vérifie que le stock disponible est suffisant pour la quantité demandée.
     * @param fkStock ID du stock
     * @param quantity Quantité demandée
     * @param fkVente ID de la vente (pour vérifier uniquement les lignes de cette vente)
     * @param ligneId ID de la ligne en cours de mise à jour (null pour création)
     */
    private void verifyStockAvailable(Long fkStock, Float quantity, Long fkVente, Long ligneId) {
        if (fkStock == null || quantity == null) {
            return;
        }

        // Récupérer le stock disponible
        // Note: La colonne réelle dans stock_produits est 'qte', pas 'stockencours'
        // 'stockencours' est un alias utilisé dans les requêtes avec JOIN
        String stockSql = "SELECT qte FROM stock_produits WHERE id = ?";
        Float stockDisponible;
        try {
            stockDisponible = jdbcTemplate.queryForObject(stockSql, Float.class, fkStock);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du stock pour ID: {} - Erreur: {}", fkStock, e.getMessage());
            throw new BusinessException("Impossible de récupérer le stock disponible");
        }

        if (stockDisponible == null) {
            throw new BusinessException("Stock introuvable pour le produit");
        }

        // Vérifier que le stock n'est pas négatif
        if (stockDisponible < 0) {
            throw new BusinessException(String.format("Le stock ne peut pas être négatif. Stock actuel: %.2f", stockDisponible));
        }

        // Récupérer toutes les lignes de vente pour ce stock dans cette vente (pour calculer la somme totale)
        // Si c'est une mise à jour, exclure la ligne en cours de modification
        Float quantiteTotaleAutresLignes = 0f;
        if (fkVente != null) {
            String lignesSql = ligneId != null
                ? "SELECT COALESCE(SUM(qt), 0) FROM lignes_vente WHERE fkStock = ? AND fkVente = ? AND id != ?"
                : "SELECT COALESCE(SUM(qt), 0) FROM lignes_vente WHERE fkStock = ? AND fkVente = ?";

            try {
                if (ligneId != null) {
                    quantiteTotaleAutresLignes = jdbcTemplate.queryForObject(lignesSql, Float.class, fkStock, fkVente, ligneId);
                } else {
                    quantiteTotaleAutresLignes = jdbcTemplate.queryForObject(lignesSql, Float.class, fkStock, fkVente);
                }
                if (quantiteTotaleAutresLignes == null) {
                    quantiteTotaleAutresLignes = 0f;
                }
            } catch (Exception e) {
                log.debug("Erreur lors du calcul de la quantité totale des autres lignes: {}", e.getMessage());
                quantiteTotaleAutresLignes = 0f;
            }
        }

        // Calculer la nouvelle quantité totale (somme de toutes les lignes pour ce produit)
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

        // Vérifier que la quantité est positive
        if (quantity <= 0) {
            throw new BusinessException("La quantité doit être supérieure à 0");
        }
    }

    private record StockProduitInfo(String produitNom, Float stockActuel) {}

    private StockProduitInfo getStockProduitInfo(Long fkStock) {
        if (fkStock == null) {
            return null;
        }
        String sql = """
                SELECT p.nomcommercial, sp.qte
                FROM stock_produits sp
                INNER JOIN produits p ON sp.fkProduits = p.id
                WHERE sp.id = ?
                """;
        try {
            return jdbcTemplate.query(sql, rs -> {
                if (!rs.next()) {
                    return null;
                }
                String nom = rs.getString("nomcommercial");
                Float qte = rs.getObject("qte") != null ? rs.getFloat("qte") : null;
                return new StockProduitInfo(nom, qte);
            }, fkStock);
        } catch (Exception e) {
            log.warn("Stock/produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
            return null;
        }
    }
}

