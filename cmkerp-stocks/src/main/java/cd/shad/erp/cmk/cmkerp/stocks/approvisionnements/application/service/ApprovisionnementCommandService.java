package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.ApprovisionnementRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.ApprovisionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.mapper.ApprovisionnementMapper;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.Approvisionnement;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.ApprovisionnementRepository;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.LigneApprovRepository;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.response.AutorisationOperationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.service.AutorisationOperationCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model.AutorisationOperation;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.service.AutorisationOperationQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * Command Service pour la gestion des approvisionnements (écriture uniquement).
 */
@Service
@Transactional
@Slf4j
public class ApprovisionnementCommandService {

    private final ApprovisionnementRepository approvisionnementRepository;
    private final LigneApprovRepository ligneApprovRepository;
    private final ApprovisionnementMapper approvisionnementMapper;
    private final AutorisationOperationCommandService autorisationOperationCommandService;
    private final AutorisationOperationQueryService autorisationOperationQueryService;
    private final JdbcTemplate jdbcTemplate;

    public ApprovisionnementCommandService(
            ApprovisionnementRepository approvisionnementRepository,
            LigneApprovRepository ligneApprovRepository,
            ApprovisionnementMapper approvisionnementMapper,
            @Lazy AutorisationOperationCommandService autorisationOperationCommandService,
            AutorisationOperationQueryService autorisationOperationQueryService,
            JdbcTemplate jdbcTemplate) {
        this.approvisionnementRepository = approvisionnementRepository;
        this.ligneApprovRepository = ligneApprovRepository;
        this.approvisionnementMapper = approvisionnementMapper;
        this.autorisationOperationCommandService = autorisationOperationCommandService;
        this.autorisationOperationQueryService = autorisationOperationQueryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Crée un nouvel approvisionnement.
     */
    public ApprovisionnementResponse create(ApprovisionnementRequest request, Long currentUserId) {
        log.debug("Création d'un nouvel approvisionnement pour la pharmacie: {}", request.getFkPharmacie());

        verifyFournisseurExists(request.getFkFournisseur());
        verifyPharmacieExists(request.getFkPharmacie());
        applyTauxFromDeviseIfNeeded(request);

        if (request.getNumbonliv() == null || request.getNumbonliv().trim().isEmpty()) {
            throw new BusinessException("Le numéro de bon de livraison est obligatoire");
        }

        int currentYear = Year.now().getValue();
        String numeroSaisi = request.getNumbonliv().trim();

        var existing = approvisionnementRepository.findByPharmacieAndNumeroBon(
                request.getFkPharmacie(), numeroSaisi, currentYear);
        if (existing.isPresent()) {
            Approvisionnement found = existing.get();
            if (found.getStatut() == Approvisionnement.StatutApprovisionnement.EN_ATTENTE) {
                log.info("Reprise du brouillon EN ATTENTE existant ID: {} (bon {})", found.getId(), numeroSaisi);
                return update(found.getId(), request, currentUserId);
            }
            if (found.isAnnule()) {
                log.info("Réactivation d'un bon annulé ID: {} (bon {}, statut {})",
                        found.getId(), numeroSaisi, found.getStatut());
                return reactivateFromAnnule(found, request, currentUserId);
            }
            throw new BusinessException(
                    "Un approvisionnement avec ce numéro de bon existe déjà (n° "
                            + found.getNumbonliv() + ", statut " + found.getStatut() + ")");
        }

        Approvisionnement approvisionnement = approvisionnementMapper.toEntity(request);
        approvisionnement.setUserCreatedId(currentUserId);
        approvisionnement.setDateCreate(LocalDateTime.now());

        int rows = approvisionnementRepository.save(approvisionnement);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de l'approvisionnement");
        }

        if (approvisionnement.getId() == null) {
            throw new BusinessException("Impossible de récupérer l'identifiant du bon créé");
        }

        String numbonlivFormate = String.format("%d-%s-%d", currentYear, numeroSaisi, approvisionnement.getId());

        approvisionnement.setNumbonliv(numbonlivFormate);
        approvisionnementRepository.update(approvisionnement);

        log.info("Approvisionnement créé avec succès: ID: {}, Numéro de bon formaté: {}",
                approvisionnement.getId(), numbonlivFormate);

        return loadResponse(approvisionnement.getId());
    }

    /**
     * Met à jour un approvisionnement existant.
     */
    public ApprovisionnementResponse update(Long id, ApprovisionnementRequest request, Long currentUserId) {
        log.debug("Mise à jour de l'approvisionnement ID: {}", id);

        Approvisionnement approvisionnement = approvisionnementRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Approvisionnement", id));

        if (approvisionnement.getStatut() == Approvisionnement.StatutApprovisionnement.VALIDEE) {
            throw new BusinessException("Impossible de modifier un approvisionnement validé");
        }
        if (approvisionnement.isAnnule()) {
            throw new BusinessException("Impossible de modifier un approvisionnement annulé");
        }

        if (request.getFkFournisseur() != null) {
            verifyFournisseurExists(request.getFkFournisseur());
        }
        if (request.getFkPharmacie() != null) {
            verifyPharmacieExists(request.getFkPharmacie());
        }

        applyTauxFromDeviseIfNeeded(request);

        approvisionnementMapper.updateEntityFromRequest(request, approvisionnement);
        approvisionnement.setUserUpdatedId(currentUserId);
        approvisionnement.setDateUpdate(LocalDateTime.now());

        int rows = approvisionnementRepository.update(approvisionnement);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de l'approvisionnement");
        }

        log.info("Approvisionnement mis à jour avec succès: ID: {}", id);
        return loadResponse(id);
    }

    private ApprovisionnementResponse reactivateFromAnnule(
            Approvisionnement approvisionnement, ApprovisionnementRequest request, Long currentUserId) {
        applyTauxFromDeviseIfNeeded(request);
        approvisionnementMapper.updateEntityFromRequest(request, approvisionnement);
        approvisionnement.setStatut(Approvisionnement.StatutApprovisionnement.EN_ATTENTE);
        approvisionnement.setUserUpdatedId(currentUserId);
        approvisionnement.setDateUpdate(LocalDateTime.now());

        if (request.getNumbonliv() != null && !request.getNumbonliv().trim().isEmpty()) {
            int currentYear = Year.now().getValue();
            String numeroSaisi = request.getNumbonliv().trim();
            approvisionnement.setNumbonliv(
                    String.format("%d-%s-%d", currentYear, numeroSaisi, approvisionnement.getId()));
        }

        int rows = approvisionnementRepository.update(approvisionnement);
        if (rows == 0) {
            throw new BusinessException("Échec de la réactivation de l'approvisionnement");
        }
        return loadResponse(approvisionnement.getId());
    }

    /**
     * Valide un approvisionnement (passe le statut à VALIDEE).
     */
    public void valider(Long id, Long currentUserId) {
        log.debug("Validation de l'approvisionnement ID: {}", id);

        Approvisionnement approvisionnement = approvisionnementRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Approvisionnement", id));

        if (approvisionnement.getFkFournisseur() == null) {
            throw new BusinessException("Un fournisseur est requis pour valider l'approvisionnement");
        }

        if (ligneApprovRepository.findByFkApprov(id).isEmpty()) {
            throw new BusinessException(
                    "Impossible de valider : au moins une ligne d'approvisionnement est requise");
        }

        approvisionnement.valider(currentUserId);

        int rows = approvisionnementRepository.update(approvisionnement);
        if (rows == 0) {
            throw new BusinessException("Échec de la validation de l'approvisionnement");
        }

        log.info("Approvisionnement validé avec succès: ID: {}", id);
    }

    /**
     * Annule un approvisionnement. Au-delà de 24h après validation, une autorisation approuvée est requise.
     */
    public void annuler(Long id, Long currentUserId) {
        Approvisionnement approvisionnement = approvisionnementRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Approvisionnement", id));

        boolean ignoreDelai = false;
        if (approvisionnement.necessiteAutorisationAnnulation()) {
            if (autorisationOperationQueryService.hasApprovedAnnulation(
                    AutorisationOperation.TABLE_APPROVISIONNEMENT, id)) {
                ignoreDelai = true;
            } else {
                throw new BusinessException(
                        "Annulation impossible au-delà de 24h. Soumettez une demande d'autorisation à l'administration.");
            }
        }

        executerAnnulation(approvisionnement, currentUserId, ignoreDelai);
    }

    /**
     * Annule après approbation admin (ignore le délai de 24h).
     * Si le stock a déjà été consommé → ANNULEE SANS MODIFICATION (pas de retrait stock).
     */
    public void annulerAvecAutorisation(Long id, Long currentUserId) {
        Approvisionnement approvisionnement = approvisionnementRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Approvisionnement", id));
        if (approvisionnement.getStatut() != Approvisionnement.StatutApprovisionnement.VALIDEE) {
            throw new BusinessException(
                    "Seul un approvisionnement validé peut être annulé après autorisation admin");
        }
        executerAnnulation(approvisionnement, currentUserId, true);
    }

    /**
     * @return true si chaque ligne a assez de stock pour un retrait (annulation classique).
     */
    private boolean stockDisponiblePourAnnulation(Long approvId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM lignes_approv la
                INNER JOIN stock_produits sp ON sp.id = la.fkStock
                WHERE la.fkApprov = ?
                  AND COALESCE(sp.qte, 0) < COALESCE(la.qt, 0)
                """,
                Integer.class,
                approvId);
        return count == null || count == 0;
    }

    /**
     * Liste lisible des lignes dont le stock ne couvre plus la quantité du bon.
     */
    private List<String> listerLignesStockInsuffisant(Long approvId) {
        return jdbcTemplate.query("""
                SELECT CONCAT(
                    COALESCE(NULLIF(TRIM(p.nomcommercial), ''), CONCAT('Produit #', p.id)),
                    ' (stock ', ROUND(COALESCE(sp.qte, 0), 0),
                    ' / requis ', ROUND(COALESCE(la.qt, 0), 0), ')'
                )
                FROM lignes_approv la
                INNER JOIN stock_produits sp ON sp.id = la.fkStock
                INNER JOIN produits p ON p.id = sp.fkProduits
                WHERE la.fkApprov = ?
                  AND COALESCE(sp.qte, 0) < COALESCE(la.qt, 0)
                ORDER BY p.nomcommercial
                """,
                (rs, rowNum) -> rs.getString(1),
                approvId);
    }

    /**
     * Crée une demande d'autorisation pour annuler un bon validé depuis plus de 24h.
     */
    public AutorisationOperationResponse demanderAnnulation(Long id, String motif, Long currentUserId) {
        Approvisionnement approvisionnement = approvisionnementRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Approvisionnement", id));

        if (approvisionnement.getStatut() != Approvisionnement.StatutApprovisionnement.VALIDEE) {
            throw new BusinessException(
                    "Seul un approvisionnement validé peut faire l'objet d'une demande d'annulation tardive");
        }
        if (approvisionnement.peutEtreAnnule()) {
            throw new BusinessException(
                    "L'annulation directe est encore possible sans autorisation administrateur");
        }
        if (autorisationOperationQueryService.hasPendingAnnulation(
                AutorisationOperation.TABLE_APPROVISIONNEMENT, id)) {
            throw new BusinessException("Une demande d'annulation est déjà en attente pour ce bon");
        }

        return autorisationOperationCommandService.creerDemande(
                AutorisationOperation.TABLE_APPROVISIONNEMENT,
                id,
                AutorisationOperation.TYPE_ANNULATION,
                motif,
                currentUserId);
    }

    private void executerAnnulation(Approvisionnement approvisionnement, Long currentUserId,
            boolean ignoreDelai) {
        Long id = approvisionnement.getId();
        log.debug("Annulation de l'approvisionnement ID: {}", id);

        // Retrait stock uniquement si le bon était VALIDEE et que le stock couvre encore les quantités.
        // EN ATTENTE : jamais de retrait (le stock n'a pas été augmenté).
        // VALIDEE + stock consommé : ANNULEE SANS MODIFICATION (pas de trigger DeleteStock).
        boolean etaitValide = approvisionnement.getStatut()
                == Approvisionnement.StatutApprovisionnement.VALIDEE;
        boolean stockOk = etaitValide && stockDisponiblePourAnnulation(id);
        boolean sansModificationStock = !stockOk;

        if (sansModificationStock) {
            List<String> manquants = listerLignesStockInsuffisant(id);
            log.warn(
                    "Annulation sans modification stock pour approv {} — stock déjà consommé : {}",
                    id,
                    String.join(" ; ", manquants));
        }

        try {
            approvisionnement.annuler(currentUserId, ignoreDelai, sansModificationStock);
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        try {
            int rows = approvisionnementRepository.update(approvisionnement);
            if (rows == 0) {
                throw new BusinessException("Échec de l'annulation de l'approvisionnement");
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            String msg = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("stock insuffisant")) {
                // Filet de sécurité : bascule en ANNULEE SANS MODIFICATION si le trigger bloque encore
                log.warn(
                        "Trigger stock insuffisant pour approv {} — bascule ANNULEE SANS MODIFICATION",
                        id);
                approvisionnement.setStatut(
                        Approvisionnement.StatutApprovisionnement.ANNULEE_SANS_MODIFICATION);
                approvisionnement.setUserUpdatedId(currentUserId);
                approvisionnement.setDateUpdate(LocalDateTime.now());
                int rows = approvisionnementRepository.update(approvisionnement);
                if (rows == 0) {
                    throw new BusinessException("Échec de l'annulation de l'approvisionnement");
                }
            } else {
                throw ex;
            }
        }

        log.info(
                "Approvisionnement annulé avec succès: ID: {} (statut={})",
                id,
                approvisionnement.getStatut());
    }

    private void applyTauxFromDeviseIfNeeded(ApprovisionnementRequest request) {
        if (request.getFkEchangeDevise() != null && request.getTaux() == null) {
            Integer taux = getTauxFromEchangeDevise(request.getFkEchangeDevise());
            request.setTaux(taux);
        }
    }

    private ApprovisionnementResponse loadResponse(Long id) {
        Approvisionnement created = approvisionnementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Approvisionnement introuvable"));

        String fournisseurNom = getFournisseurNom(created.getFkFournisseur());
        String pharmacieNom = getPharmacieNom(created.getFkPharmacie());
        String echangeDeviseMonnaie = created.getFkEchangeDevise() != null
                ? getEchangeDeviseMonnaie(created.getFkEchangeDevise())
                : null;

        ApprovisionnementResponse response = approvisionnementMapper.toResponse(created, fournisseurNom,
                pharmacieNom, echangeDeviseMonnaie);
        enrichAutorisationFlags(created, response);
        response.setUserCreateNom(getUserDisplayName(created.getUserCreatedId()));
        response.setUserUpdateNom(getUserDisplayName(created.getUserUpdatedId()));
        return response;
    }

    private void enrichAutorisationFlags(Approvisionnement entity, ApprovisionnementResponse response) {
        response.setNecessiteAutorisationAnnulation(entity.necessiteAutorisationAnnulation());
        response.setDemandeAnnulationEnCours(
                autorisationOperationQueryService.hasPendingAnnulation(
                        AutorisationOperation.TABLE_APPROVISIONNEMENT, entity.getId()));
    }

    private void verifyFournisseurExists(Long fkFournisseur) {
        String sql = "SELECT COUNT(*) FROM fournisseurs WHERE id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, fkFournisseur);
        if (count == null || count == 0) {
            throw NotFoundException.entity("Fournisseur", fkFournisseur);
        }
    }

    private void verifyPharmacieExists(Long fkPharmacie) {
        String sql = "SELECT COUNT(*) FROM pharmacies WHERE id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, fkPharmacie);
        if (count == null || count == 0) {
            throw NotFoundException.entity("Pharmacie", fkPharmacie);
        }
    }

    private Integer getTauxFromEchangeDevise(Long fkEchangeDevise) {
        String sql = "SELECT tauxechange FROM echange_devise WHERE id = ?";
        try {
            Float tauxFloat = jdbcTemplate.queryForObject(sql, Float.class, fkEchangeDevise);
            return tauxFloat != null ? Math.round(tauxFloat) : null;
        } catch (Exception e) {
            log.warn("EchangeDevise non trouvé pour ID: {}", fkEchangeDevise);
            return null;
        }
    }

    private String getFournisseurNom(Long fkFournisseur) {
        if (fkFournisseur == null) {
            return null;
        }
        String sql = "SELECT nom FROM fournisseurs WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, fkFournisseur);
        } catch (Exception e) {
            log.warn("Fournisseur non trouvé pour ID: {}", fkFournisseur);
            return null;
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

    private String getEchangeDeviseMonnaie(Long fkEchangeDevise) {
        if (fkEchangeDevise == null) {
            return null;
        }
        String sql = "SELECT monnaieechange FROM echange_devise WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, fkEchangeDevise);
        } catch (Exception e) {
            log.warn("EchangeDevise non trouvé pour ID: {}", fkEchangeDevise);
            return null;
        }
    }

    private String getUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return jdbcTemplate.query(
                    "SELECT prenom, nom, username FROM utilisateurs WHERE id = ?",
                    rs -> {
                        if (!rs.next()) {
                            return null;
                        }
                        String prenom = rs.getString("prenom");
                        String nom = rs.getString("nom");
                        String username = rs.getString("username");
                        String full = String.join(" ",
                                prenom != null ? prenom.trim() : "",
                                nom != null ? nom.trim() : "").trim();
                        if (!full.isEmpty()) {
                            return full;
                        }
                        return username != null && !username.isBlank() ? username.trim() : null;
                    },
                    userId);
        } catch (Exception e) {
            log.warn("Utilisateur non trouvé pour ID: {}", userId);
            return null;
        }
    }
}
