package cd.shad.erp.cmk.cmkerp.platform.site.application.service;

import java.time.LocalDateTime;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.SiteRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.SiteResponse;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.model.Site;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.repository.SiteRepository;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.service.SiteDomainService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des sites (écriture uniquement).
 *
 * <p>
 * Ce service contient toutes les opérations de modification (commands) liées aux sites. Toutes les
 * méthodes modifient l'état du système et invalident le cache.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SiteCommandService {

  private final SiteRepository siteRepository;
  private final SiteDomainService siteDomainService;

  /**
   * Crée un nouveau site. Invalide le cache "all" après création.
   */
  @CacheEvict(value = "sites", key = "'all'")
  public SiteResponse create(SiteRequest request, Long currentUserId) {
    log.debug("Création d'un nouveau site: {}", request.getDesignation());

    // Validation métier via Domain Service
    siteDomainService.validerCreationSite(request.getDesignation());

    // Créer l'agrégat Site
    Site site = Site.builder().designation(request.getDesignation())
        .abbreviation(request.getAbbreviation()).adresse(request.getAdresse())
        .bloquer(request.getBloquer() != null ? request.getBloquer() : false)
        .userCreatedId(currentUserId).dateCreate(LocalDateTime.now()).build();

    // Utiliser les méthodes métier de l'agrégat
    site.changerDesignation(request.getDesignation()); // Valide et met à jour

    // Sauvegarder via le repository
    int rows = siteRepository.save(site);
    if (rows == 0) {
      throw new BusinessException("Échec de la création du site");
    }

    // Récupérer le site créé avec son ID
    Site created = siteRepository.findByDesignation(request.getDesignation())
        .orElseThrow(() -> new BusinessException("Erreur lors de la récupération du site créé"));

    log.info("Site créé avec succès: ID={}, designation={}", created.getId(),
        created.getDesignation());
    return siteToResponse(created);
  }

  /**
   * Met à jour un site existant. Invalide les caches du site modifié et de la liste complète.
   */
  @CacheEvict(value = "sites", key = "#id + 'all'")
  public SiteResponse update(Long id, SiteRequest request, Long currentUserId) {
    log.debug("Mise à jour du site ID: {}", id);

    Site site = siteRepository.findById(id).orElseThrow(() -> NotFoundException.entity("Site", id));

    // Validation métier via Domain Service si la désignation change
    if (request.getDesignation() != null
        && !request.getDesignation().equals(site.getDesignation())) {
      siteDomainService.validerModificationSite(site, request.getDesignation());
      site.changerDesignation(request.getDesignation()); // Utilise la méthode métier
    }

    if (request.getAbbreviation() != null) {
      site.changerAbbreviation(request.getAbbreviation());
    }
    if (request.getAdresse() != null) {
      site.changerAdresse(request.getAdresse());
    }
    if (request.getBloquer() != null) {
      if (request.getBloquer()) {
        site.bloquer();
      } else {
        site.debloquer();
      }
    }

    site.setUserUpdatedId(currentUserId);
    site.setDateUpdate(LocalDateTime.now());

    // Sauvegarder via le repository
    int rows = siteRepository.update(site);
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour du site");
    }

    log.info("Site mis à jour avec succès: ID={}", site.getId());
    return siteToResponse(site);
  }

  /**
   * Supprime un site. Invalide les caches du site supprimé et de la liste complète.
   */
  @CacheEvict(value = "sites", key = "#id + 'all'")
  public void deleteById(Long id) {
    log.debug("Suppression du site ID: {}", id);

    siteRepository.findById(id).orElseThrow(() -> NotFoundException.entity("Site", id));

    int rows = siteRepository.deleteById(id);
    if (rows == 0) {
      throw new BusinessException("Échec de la suppression du site");
    }

    log.info("Site supprimé avec succès: ID={}", id);
  }

  /**
   * Convertit un Site (domain) en SiteResponse (DTO).
   */
  private SiteResponse siteToResponse(Site site) {
    if (site == null) {
      return null;
    }

    return SiteResponse.builder().id(site.getId()).designation(site.getDesignation())
        .abbreviation(site.getAbbreviation()).adresse(site.getAdresse()).bloquer(site.getBloquer())
        .dateCreate(site.getDateCreate()).dateUpdate(site.getDateUpdate())
        .userCreatedId(site.getUserCreatedId()).userUpdatedId(site.getUserUpdatedId()).build();
  }
}

