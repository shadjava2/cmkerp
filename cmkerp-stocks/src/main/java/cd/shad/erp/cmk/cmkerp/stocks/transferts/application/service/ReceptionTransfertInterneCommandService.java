package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateLigneReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.ReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.ReceptionTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.LigneReceptionTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.ReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.ReceptionTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.LigneReceptionTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.TransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.LigneTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Command Service pour la gestion des réceptions de transferts internes (écriture uniquement).
 */
@Service
@Transactional
@Slf4j
public class ReceptionTransfertInterneCommandService {

    private final ReceptionTransfertInterneRepository receptionTransfertInterneRepository;
    private final LigneReceptionTransfertInterneRepository ligneReceptionTransfertInterneRepository;
    private final TransfertInterneRepository transfertInterneRepository;
    private final LigneTransfertInterneRepository ligneTransfertInterneRepository;
    private final ReceptionTransfertInterneMapper receptionTransfertInterneMapper;
    private final LigneReceptionTransfertInterneMapper ligneReceptionTransfertInterneMapper;
    private final ReceptionTransfertInterneQueryService receptionTransfertInterneQueryService;
    private final JdbcTemplate jdbcTemplate;

    public ReceptionTransfertInterneCommandService(
            ReceptionTransfertInterneRepository receptionTransfertInterneRepository,
            LigneReceptionTransfertInterneRepository ligneReceptionTransfertInterneRepository,
            TransfertInterneRepository transfertInterneRepository,
            LigneTransfertInterneRepository ligneTransfertInterneRepository,
            ReceptionTransfertInterneMapper receptionTransfertInterneMapper,
            LigneReceptionTransfertInterneMapper ligneReceptionTransfertInterneMapper,
            ReceptionTransfertInterneQueryService receptionTransfertInterneQueryService,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.receptionTransfertInterneRepository = receptionTransfertInterneRepository;
        this.ligneReceptionTransfertInterneRepository = ligneReceptionTransfertInterneRepository;
        this.transfertInterneRepository = transfertInterneRepository;
        this.ligneTransfertInterneRepository = ligneTransfertInterneRepository;
        this.receptionTransfertInterneMapper = receptionTransfertInterneMapper;
        this.ligneReceptionTransfertInterneMapper = ligneReceptionTransfertInterneMapper;
        this.receptionTransfertInterneQueryService = receptionTransfertInterneQueryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Crée une nouvelle réception de transfert interne.
     * Les lignes sont automatiquement créées à partir des lignes du transfert interne si non fournies.
     */
    public ReceptionTransfertInterneResponse create(CreateReceptionTransfertInterneRequest request, Long currentUserId) {
        log.debug("Création d'une réception de transfert interne pour transfert: {}", request.getFkTransfertInterne());

        // Vérifier que le transfert interne existe
        TransfertInterne transfert = transfertInterneRepository.findById(request.getFkTransfertInterne())
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", request.getFkTransfertInterne()));

        // Vérifier que le transfert interne est TRANSFEREE
        if (transfert.getStatut() != TransfertInterne.StatutTransfertInterne.TRANSFEREE) {
            throw new BusinessException("Impossible de réceptionner un transfert interne qui n'est pas TRANSFEREE");
        }

        // Vérifier qu'il n'existe pas déjà une réception active (non annulée) pour ce transfert
        Optional<ReceptionTransfertInterne> existingReception = receptionTransfertInterneRepository.findByFkTransfertInterne(request.getFkTransfertInterne());
        if (existingReception.isPresent()) {
            ReceptionTransfertInterne reception = existingReception.get();
            // Permettre la création si la réception existante est annulée
            if (reception.getStatut() != ReceptionTransfertInterne.StatutReceptionTransfertInterne.ANNULEE) {
                throw new BusinessException("Une réception active existe déjà pour ce transfert interne");
            }
            // Si la réception est annulée, on peut créer une nouvelle réception
            log.info("Une réception annulée existe déjà pour ce transfert, création d'une nouvelle réception");
        }

        // Créer la réception
        ReceptionTransfertInterne reception = receptionTransfertInterneMapper.toEntity(request);
        // Copier perime depuis le transfert_interne
        reception.setPerime(transfert.getPerime());
        reception.setUserCreatedId(currentUserId);
        reception.setDateCreate(LocalDateTime.now());

        log.debug("Tentative de création de la réception - fkTransfertInterne: {}, statut: {}, userCreatedId: {}",
                reception.getFkTransfertInterne(), reception.getStatut(), currentUserId);

        int rows;
        try {
            rows = receptionTransfertInterneRepository.save(reception);
        } catch (Exception e) {
            log.error("Erreur lors de la création de la réception - fkTransfertInterne: {}, erreur: {}",
                    reception.getFkTransfertInterne(), e.getMessage(), e);
            throw new BusinessException("Erreur lors de la création de la réception: " + e.getMessage(), e);
        }

        if (rows == 0) {
            log.error("Aucune ligne affectée lors de la création de la réception - fkTransfertInterne: {}",
                    reception.getFkTransfertInterne());
            throw new BusinessException("Échec de la création de la réception de transfert interne");
        }

        if (reception.getId() == null) {
            log.error("ID de réception non généré après création - fkTransfertInterne: {}",
                    reception.getFkTransfertInterne());
            throw new BusinessException("Échec de la création de la réception: ID non généré");
        }

        log.info("Réception de transfert interne créée avec succès: ID: {}", reception.getId());

        // Créer les lignes de réception
        if (request.getLignes() != null && !request.getLignes().isEmpty()) {
            // Utiliser les lignes fournies dans la requête
            for (CreateLigneReceptionTransfertInterneRequest ligneRequest : request.getLignes()) {
                createLigneReception(reception.getId(), ligneRequest, currentUserId);
            }
        } else {
            // Créer automatiquement les lignes à partir des lignes du transfert interne
            List<LigneTransfertInterne> lignesTransfert = ligneTransfertInterneRepository.findByFkTransfertInterne(request.getFkTransfertInterne());

            if (lignesTransfert.isEmpty()) {
                throw new BusinessException("Le transfert interne ne contient aucune ligne");
            }

            for (LigneTransfertInterne ligneTransfert : lignesTransfert) {
                CreateLigneReceptionTransfertInterneRequest ligneRequest = CreateLigneReceptionTransfertInterneRequest.builder()
                        .fkStock(ligneTransfert.getFkStock())
                        .fkAlertePeremption(ligneTransfert.getFkAlertePeremption()) // Copier fkAlertePeremption depuis la ligne de transfert
                        .quantiteDemandee(ligneTransfert.getQuantite()) // Quantité demandée = quantité du transfert
                        .quantiteTransferee(ligneTransfert.getQuantite()) // Quantité transférée = quantité du transfert
                        .quantite(ligneTransfert.getQuantite()) // Quantité à réceptionner = quantité du transfert par défaut
                        .build();
                createLigneReception(reception.getId(), ligneRequest, currentUserId);
            }
        }

        // Récupérer la réception créée
        return receptionTransfertInterneQueryService.findById(reception.getId());
    }

    /**
     * Crée une ligne de réception.
     */
    private void createLigneReception(Long fkReceptionTransfertInterne, CreateLigneReceptionTransfertInterneRequest request, Long currentUserId) {
        log.debug("Création d'une ligne de réception - fkReceptionTransfertInterne: {}, fkStock: {}, quantite: {}",
                fkReceptionTransfertInterne, request.getFkStock(), request.getQuantite());

        // Vérifier que le produit n'est pas périmé si une quantité est fournie
        if (request.getQuantite() != null && request.getQuantite() > 0) {
            String peremption = getPeremption(request.getFkStock());
            if (peremption != null && !peremption.trim().isEmpty()) {
                String[] dates = peremption.split(",");
                java.time.LocalDate today = java.time.LocalDate.now();
                for (String dateStr : dates) {
                    try {
                        java.time.LocalDate datePeremption = java.time.LocalDate.parse(dateStr.trim());
                        if (datePeremption.isBefore(today) || datePeremption.isEqual(today)) {
                            throw new BusinessException("Impossible de créer une ligne de réception pour un produit périmé. Date de péremption: " + dateStr.trim());
                        }
                    } catch (java.time.format.DateTimeParseException e) {
                        log.warn("Erreur lors de la vérification de la date de péremption: {}", dateStr, e);
                    }
                }
            }
        }

        LigneReceptionTransfertInterne ligne = ligneReceptionTransfertInterneMapper.toEntity(request, fkReceptionTransfertInterne);
        ligne.setUserCreatedId(currentUserId);
        ligne.setDateCreate(LocalDateTime.now());

        int rows;
        try {
            rows = ligneReceptionTransfertInterneRepository.save(ligne);
        } catch (Exception e) {
            log.error("Erreur lors de la création de la ligne de réception - fkReceptionTransfertInterne: {}, fkStock: {}, erreur: {}",
                    fkReceptionTransfertInterne, request.getFkStock(), e.getMessage(), e);
            throw new BusinessException("Erreur lors de la création de la ligne de réception: " + e.getMessage(), e);
        }

        if (rows == 0) {
            log.error("Aucune ligne affectée lors de la création de la ligne de réception - fkReceptionTransfertInterne: {}, fkStock: {}",
                    fkReceptionTransfertInterne, request.getFkStock());
            throw new BusinessException("Échec de la création de la ligne de réception");
        }

        if (ligne.getId() == null) {
            log.error("ID de ligne non généré après création - fkReceptionTransfertInterne: {}, fkStock: {}",
                    fkReceptionTransfertInterne, request.getFkStock());
            throw new BusinessException("Échec de la création de la ligne de réception: ID non généré");
        }

        log.debug("Ligne de réception créée avec succès: ID: {}", ligne.getId());
    }

    /**
     * Réceptionne le transfert interne (passe le statut à RECEPTIONNEE).
     * Met également à jour le statut du transfert interne à RECEPTIONNEE.
     */
    public ReceptionTransfertInterneResponse receptionner(Long receptionId, Long currentUserId) {
        log.debug("Réception du transfert interne - réception ID: {}", receptionId);

        ReceptionTransfertInterne reception = receptionTransfertInterneRepository.findById(receptionId)
                .orElseThrow(() -> NotFoundException.entity("ReceptionTransfertInterne", receptionId));

        // Vérifier le statut de la réception AVANT toute autre opération
        if (reception.getStatut() == ReceptionTransfertInterne.StatutReceptionTransfertInterne.RECEPTIONNEE) {
            throw new BusinessException("Cette réception est déjà réceptionnée. Impossible de la réceptionner à nouveau.");
        }
        if (reception.getStatut() == ReceptionTransfertInterne.StatutReceptionTransfertInterne.ANNULEE) {
            throw new BusinessException("Impossible de réceptionner une réception annulée.");
        }
        if (reception.getStatut() != ReceptionTransfertInterne.StatutReceptionTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Le statut de la réception ne permet pas la réception. Statut actuel: " + reception.getStatut().getDbValue());
        }

        // Vérifier qu'aucune ligne ne contient de produit périmé AVANT de réceptionner
        List<LigneReceptionTransfertInterne> lignes = ligneReceptionTransfertInterneRepository.findByFkReceptionTransfertInterne(reception.getId());

        // Vérifier qu'il y a au moins une ligne avec une quantité > 0
        boolean hasValidLigne = false;
        for (LigneReceptionTransfertInterne ligne : lignes) {
            if (ligne.getQuantite() != null && ligne.getQuantite() > 0) {
                hasValidLigne = true;

                // Vérifier que le produit n'est pas périmé
                String peremption = getPeremption(ligne.getFkStock());
                if (peremption != null && !peremption.trim().isEmpty()) {
                    String[] dates = peremption.split(",");
                    java.time.LocalDate today = java.time.LocalDate.now();
                    for (String dateStr : dates) {
                        try {
                            java.time.LocalDate datePeremption = java.time.LocalDate.parse(dateStr.trim());
                            if (datePeremption.isBefore(today) || datePeremption.isEqual(today)) {
                                throw new BusinessException("Impossible de réceptionner: au moins un produit est périmé. Date de péremption: " + dateStr.trim());
                            }
                        } catch (java.time.format.DateTimeParseException e) {
                            log.warn("Erreur lors de la vérification de la date de péremption: {}", dateStr, e);
                        }
                    }
                }
            }
        }

        if (!hasValidLigne) {
            throw new BusinessException("Impossible de réceptionner: au moins une ligne avec une quantité > 0 est obligatoire");
        }

        // Mettre à jour les stocks (ajouter les quantités réceptionnées à la pharmacie destination)
        // Faire cela AVANT de mettre à jour les statuts pour éviter les incohérences en cas d'erreur
        try {
            updateStocksAfterReception(reception, currentUserId);
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des stocks pour réception ID: {} - Erreur: {}", receptionId, e.getMessage(), e);
            throw new BusinessException("Erreur lors de la mise à jour des stocks: " + e.getMessage(), e);
        }

        // Réceptionner la réception (mise à jour du statut)
        reception.receptionner(currentUserId);
        int rows = receptionTransfertInterneRepository.update(reception);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour du statut de la réception");
        }

        // Mettre à jour le statut du transfert interne à RECEPTIONNEE
        TransfertInterne transfert = transfertInterneRepository.findById(reception.getFkTransfertInterne())
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", reception.getFkTransfertInterne()));

        // Vérifier le statut du transfert avant de le réceptionner
        if (transfert.getStatut() == TransfertInterne.StatutTransfertInterne.RECEPTIONNEE) {
            log.warn("Le transfert interne {} est déjà réceptionné, mais la réception n'était pas marquée comme réceptionnée", transfert.getId());
            // Ne pas lever d'exception, juste logger un avertissement
        } else {
            try {
                transfert.receptionner(currentUserId);
                transfertInterneRepository.update(transfert);
            } catch (IllegalStateException e) {
                // Convertir l'IllegalStateException en BusinessException pour un message plus clair
                throw new BusinessException("Impossible de réceptionner le transfert interne: " + e.getMessage(), e);
            }
        }

        log.info("Transfert interne réceptionné avec succès: réception ID: {}", receptionId);

        return receptionTransfertInterneQueryService.findById(receptionId);
    }

    /**
     * Annule la réception (passe le statut à ANNULEE).
     */
    public ReceptionTransfertInterneResponse annuler(Long receptionId, Long currentUserId) {
        log.debug("Annulation de la réception - réception ID: {}", receptionId);

        ReceptionTransfertInterne reception = receptionTransfertInterneRepository.findById(receptionId)
                .orElseThrow(() -> NotFoundException.entity("ReceptionTransfertInterne", receptionId));

        reception.annuler(currentUserId);
        int rows = receptionTransfertInterneRepository.update(reception);
        if (rows == 0) {
            throw new BusinessException("Échec de l'annulation");
        }

        log.info("Réception annulée avec succès: réception ID: {}", receptionId);

        return receptionTransfertInterneQueryService.findById(receptionId);
    }

    /**
     * Met à jour les stocks après réception.
     * Ajoute les quantités à la pharmacie destination. La déduction source est faite à la validation
     * du transfert ; une déduction conditionnelle reste pour les transferts validés avant ce correctif.
     */
    private void updateStocksAfterReception(ReceptionTransfertInterne reception, Long currentUserId) {
        log.debug("Mise à jour des stocks après réception - réception ID: {}", reception.getId());

        List<LigneReceptionTransfertInterne> lignes = ligneReceptionTransfertInterneRepository.findByFkReceptionTransfertInterne(reception.getId());

        TransfertInterne transfert = transfertInterneRepository.findById(reception.getFkTransfertInterne())
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", reception.getFkTransfertInterne()));

        Long fkPharmacieDestination = transfert.getFkPharmacieDestination();
        Long fkPharmacieSource = transfert.getFkPharmacieSource();

        log.debug("Pharmacie destination: {}, Pharmacie source: {}", fkPharmacieDestination, fkPharmacieSource);

        for (LigneReceptionTransfertInterne ligne : lignes) {
            if (ligne.getQuantite() != null && ligne.getQuantite() > 0) {
                try {
                    // Récupérer le fkProduits du stock source
                    String getFkProduitsSql = "SELECT fkProduits FROM stock_produits WHERE id = ?";
                    Long fkProduits;
                    try {
                        fkProduits = jdbcTemplate.queryForObject(getFkProduitsSql, Long.class, ligne.getFkStock());
                    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                        log.error("Stock source introuvable - stock ID: {}", ligne.getFkStock());
                        throw new BusinessException("Stock source introuvable pour la ligne de réception (stock ID: " + ligne.getFkStock() + ")");
                    }

                    if (fkProduits == null) {
                        log.error("fkProduits est null pour le stock ID: {}", ligne.getFkStock());
                        throw new BusinessException("Produit introuvable pour le stock ID: " + ligne.getFkStock());
                    }

                    log.debug("Traitement ligne - stock ID: {}, fkProduits: {}, quantité: {}",
                            ligne.getFkStock(), fkProduits, ligne.getQuantite());

                    // Trouver ou créer le stock de destination
                    String findStockDestinationSql = "SELECT id FROM stock_produits WHERE fkProduits = ? AND fkPharmacies = ?";
                    Long stockDestinationId = null;
                    try {
                        stockDestinationId = jdbcTemplate.queryForObject(findStockDestinationSql, Long.class, fkProduits, fkPharmacieDestination);
                        log.debug("Stock destination trouvé - ID: {}", stockDestinationId);
                    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                        // Le stock n'existe pas, on va le créer
                        log.debug("Stock destination n'existe pas, création en cours...");
                    }

                    if (stockDestinationId != null) {
                        // Mettre à jour le stock existant de la destination
                        String updateStockSql = """
                            UPDATE stock_produits
                            SET qte = qte + ?,
                                dateupdate = CURRENT_TIMESTAMP,
                                userupdateid = ?
                            WHERE id = ?
                            """;
                        int rowsUpdated = jdbcTemplate.update(updateStockSql, ligne.getQuantite(), currentUserId, stockDestinationId);
                        if (rowsUpdated == 0) {
                            log.error("Échec de la mise à jour du stock destination - stock ID: {}", stockDestinationId);
                            throw new BusinessException("Échec de la mise à jour du stock destination");
                        }
                        log.debug("Stock destination mis à jour - ID: {}, quantité ajoutée: {}", stockDestinationId, ligne.getQuantite());
                    } else {
                        // Créer le stock pour la pharmacie destination
                        String insertStockSql = """
                            INSERT INTO stock_produits (fkProduits, fkPharmacies, qte, datecreate, usercreateid)
                            VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)
                            """;
                        int rowsInserted = jdbcTemplate.update(insertStockSql, fkProduits, fkPharmacieDestination, ligne.getQuantite(), currentUserId);
                        if (rowsInserted == 0) {
                            log.error("Échec de la création du stock destination - fkProduits: {}, fkPharmacies: {}",
                                    fkProduits, fkPharmacieDestination);
                            throw new BusinessException("Échec de la création du stock destination");
                        }
                        log.debug("Stock destination créé - fkProduits: {}, fkPharmacies: {}, quantité: {}",
                                fkProduits, fkPharmacieDestination, ligne.getQuantite());
                    }

                    // Déduction source si pas encore faite à la validation (transferts historiques)
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
                        log.debug("Stock source déjà déduit à la validation - stock ID: {}", ligne.getFkStock());
                    } else {
                        log.debug("Stock source réduit à la réception (legacy) - stock ID: {}, quantité: {}",
                                ligne.getFkStock(), ligne.getQuantite());
                    }

                } catch (org.springframework.dao.DataAccessException e) {
                    log.error("Erreur SQL lors de la mise à jour des stocks pour ligne ID: {} - Erreur: {}",
                            ligne.getId(), e.getMessage(), e);
                    throw new BusinessException("Erreur lors de la mise à jour des stocks: " + e.getMessage(), e);
                } catch (BusinessException e) {
                    // Re-lancer les BusinessException telles quelles
                    throw e;
                } catch (Exception e) {
                    log.error("Erreur inattendue lors de la mise à jour des stocks pour ligne ID: {} - Erreur: {}",
                            ligne.getId(), e.getMessage(), e);
                    throw new BusinessException("Erreur inattendue lors de la mise à jour des stocks: " + e.getMessage(), e);
                }
            }
        }

        log.debug("Mise à jour des stocks terminée avec succès pour réception ID: {}", reception.getId());
    }

    /**
     * Récupère la péremption d'un stock.
     */
    private String getPeremption(Long fkStock) {
        if (fkStock == null) {
            return null;
        }
        String sql = """
            SELECT GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE fkStock = ? AND notifactif = TRUE
            GROUP BY fkStock
            """;
        try {
            return jdbcTemplate.queryForObject(sql, String.class, fkStock);
        } catch (Exception e) {
            // Pas de péremption enregistrée
            return null;
        }
    }
}

