package cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieOverviewResponse;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.infrastructure.persistence.PharmacieDashboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query Service pour le dashboard Pharmacie (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées au dashboard Pharmacie.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PharmacieDashboardQueryService {

    private final PharmacieDashboardRepository pharmacieDashboardRepository;

    /**
     * Recherche paginée de pharmacies avec métriques pour un utilisateur.
     *
     * <p>
     * Retourne une page de pharmacies avec :
     * <ul>
     * <li>Informations de base (pharmacie + site)</li>
     * <li>Indicateur d'accès de l'utilisateur</li>
     * <li>Nombre d'utilisateurs ayant accès</li>
     * <li>Nombre de notifications en attente</li>
     * </ul>
     *
     * @param userId l'ID de l'utilisateur courant
     * @param siteId filtre optionnel sur le site (null = pas de filtre)
     * @param typePharmacie filtre optionnel sur le type de pharmacie (null = pas de filtre)
     * @param searchText filtre optionnel sur designation ou site.nom (null = pas de filtre)
     * @param pageable paramètres de pagination (page, size)
     * @return Page de PharmacieOverviewResponse
     */
    public Page<PharmacieOverviewResponse> searchPharmacies(
            Long userId,
            Long siteId,
            String typePharmacie,
            String searchText,
            Pageable pageable) {

        log.debug("Recherche de pharmacies pour utilisateur ID: {}, siteId: {}, typePharmacie: {}, searchText: {}",
                userId, siteId, typePharmacie, searchText);

        // Validation: l'utilisateur doit être fourni
        if (userId == null) {
            log.warn("Recherche de pharmacies avec userId null - retour d'une page vide");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        // Récupération des résultats paginés
        var pharmacies = pharmacieDashboardRepository.searchPharmaciesForUser(
                userId, siteId, typePharmacie, searchText, limit, offset);

        // Comptage du total (sans pagination)
        long total = pharmacieDashboardRepository.countPharmaciesForUser(
                userId, siteId, typePharmacie, searchText);

        log.debug("Recherche terminée: {} pharmacies trouvées sur {} total pour utilisateur ID: {}",
                pharmacies.size(), total, userId);

        // Avertissement si aucune pharmacie trouvée (peut indiquer un problème de droits)
        if (total == 0 && log.isWarnEnabled()) {
            log.warn("Aucune pharmacie trouvée pour l'utilisateur ID: {} - Vérifier les droits dans droits_pharmacies", userId);
        }

        return new PageImpl<>(pharmacies, pageable, total);
    }
}

