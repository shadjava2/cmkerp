package cd.shad.erp.cmk.cmkerp.platform.site.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.SiteResponse;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.model.Site;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.repository.SiteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des sites (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées aux sites.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SiteQueryService {

    private final SiteRepository siteRepository;

    /**
     * Récupère tous les sites.
     * Résultats mis en cache pour améliorer les performances.
     */
    @Cacheable(value = "sites", key = "'all'")
    public List<SiteResponse> findAll() {
        log.debug("Récupération de tous les sites");
        return siteRepository.findAll().stream()
                .map(this::siteToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un site par son ID.
     * Résultat mis en cache avec la clé basée sur l'ID.
     */
    @Cacheable(value = "sites", key = "#id")
    public SiteResponse findById(Long id) {
        log.debug("Récupération du site ID: {}", id);
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Site", id));

        return siteToResponse(site);
    }

    /**
     * Convertit un Site (domain) en SiteResponse (DTO).
     */
    private SiteResponse siteToResponse(Site site) {
        if (site == null) {
            return null;
        }

        return SiteResponse.builder()
                .id(site.getId())
                .designation(site.getDesignation())
                .abbreviation(site.getAbbreviation())
                .adresse(site.getAdresse())
                .bloquer(site.getBloquer())
                .dateCreate(site.getDateCreate())
                .dateUpdate(site.getDateUpdate())
                .build();
    }
}

