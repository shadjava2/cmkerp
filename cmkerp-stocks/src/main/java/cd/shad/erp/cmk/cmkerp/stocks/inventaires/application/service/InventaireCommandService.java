package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.InventaireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.InventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.mapper.InventaireMapper;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.Inventaire;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository.InventaireRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Command Service pour la gestion des inventaires (écriture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventaireCommandService {

    private final InventaireRepository inventaireRepository;
    private final InventaireMapper inventaireMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Crée un nouvel inventaire.
     * Note: Les lignes sont créées automatiquement par une procédure stockée.
     */
    public InventaireResponse create(InventaireRequest request, Long currentUserId) {
        log.debug("Création d'un nouvel inventaire pour la pharmacie: {}", request.getFkPharmacie());

        // Vérifier que la pharmacie existe
        verifyPharmacieExists(request.getFkPharmacie());

        // Créer l'agrégat Inventaire
        Inventaire inventaire = inventaireMapper.toEntity(request);
        inventaire.setUserCreatedId(currentUserId);
        inventaire.setDateCreate(LocalDateTime.now());

        // Sauvegarder
        int rows = inventaireRepository.save(inventaire);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de l'inventaire");
        }

        log.info("Inventaire créé avec succès: ID: {}", inventaire.getId());

        // Note: Les lignes sont créées automatiquement par une procédure stockée
        // qui est déclenchée après l'insertion de l'inventaire

        // Récupérer l'inventaire créé avec les désignations
        Inventaire created = inventaireRepository.findById(inventaire.getId())
                .orElseThrow(() -> new BusinessException("Inventaire créé mais introuvable"));

        String pharmacieNom = getPharmacieNom(created.getFkPharmacie());

        return inventaireMapper.toResponse(created, pharmacieNom);
    }

    /**
     * Met à jour un inventaire existant.
     */
    public InventaireResponse update(Long id, InventaireRequest request, Long currentUserId) {
        log.debug("Mise à jour de l'inventaire ID: {}", id);

        Inventaire inventaire = inventaireRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Inventaire", id));

        // Vérifier que l'inventaire peut être modifié (pas terminé ou annulé)
        if (inventaire.getStatut() == Inventaire.StatutInventaire.TERMINE) {
            throw new BusinessException("Impossible de modifier un inventaire terminé");
        }
        if (inventaire.getStatut() == Inventaire.StatutInventaire.ANNULE) {
            throw new BusinessException("Impossible de modifier un inventaire annulé");
        }

        // Vérifier les références si fournies
        if (request.getFkPharmacie() != null) {
            verifyPharmacieExists(request.getFkPharmacie());
        }

        // Mettre à jour l'entité
        inventaireMapper.updateEntityFromRequest(request, inventaire);
        inventaire.setUserUpdatedId(currentUserId);
        inventaire.setDateUpdate(LocalDateTime.now());

        int rows = inventaireRepository.update(inventaire);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de l'inventaire");
        }

        log.info("Inventaire mis à jour avec succès: ID: {}", id);

        // Récupérer l'inventaire mis à jour avec les désignations
        Inventaire updated = inventaireRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Inventaire mis à jour mais introuvable"));

        String pharmacieNom = getPharmacieNom(updated.getFkPharmacie());

        return inventaireMapper.toResponse(updated, pharmacieNom);
    }

    /**
     * Termine un inventaire (passe le statut à TERMINE et met à jour date_fin).
     */
    public void terminer(Long id, Long currentUserId) {
        log.debug("Terminaison de l'inventaire ID: {}", id);

        Inventaire inventaire = inventaireRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Inventaire", id));

        inventaire.terminer(currentUserId);

        int rows = inventaireRepository.update(inventaire);
        if (rows == 0) {
            throw new BusinessException("Échec de la terminaison de l'inventaire");
        }

        log.info("Inventaire terminé avec succès: ID: {}", id);
    }

    /**
     * Annule un inventaire (passe le statut à ANNULE).
     */
    public void annuler(Long id, Long currentUserId) {
        log.debug("Annulation de l'inventaire ID: {}", id);

        Inventaire inventaire = inventaireRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Inventaire", id));

        inventaire.annuler(currentUserId);

        int rows = inventaireRepository.update(inventaire);
        if (rows == 0) {
            throw new BusinessException("Échec de l'annulation de l'inventaire");
        }

        log.info("Inventaire annulé avec succès: ID: {}", id);
    }

    private void verifyPharmacieExists(Long fkPharmacie) {
        String sql = "SELECT COUNT(*) FROM pharmacies WHERE id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, fkPharmacie);
        if (count == null || count == 0) {
            throw NotFoundException.entity("Pharmacie", fkPharmacie);
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

