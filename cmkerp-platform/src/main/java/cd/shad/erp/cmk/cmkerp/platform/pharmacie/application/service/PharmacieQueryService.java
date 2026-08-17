package cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model.Pharmacie;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository.PharmacieRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.DroitPharmacie;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.DroitPharmacieRepository;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.repository.SiteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des pharmacies (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées aux pharmacies.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PharmacieQueryService {

    private final PharmacieRepository pharmacieRepository;
    private final SiteRepository siteRepository;
    private final DroitPharmacieRepository droitPharmacieRepository;

    /**
     * Récupère toutes les pharmacies.
     */
    public List<PharmacieResponse> findAll() {
        log.debug("Récupération de toutes les pharmacies");
        return pharmacieRepository.findAll().stream()
                .map(this::pharmacieToResponseWithSite)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une pharmacie par son ID.
     */
    public PharmacieResponse findById(Long id) {
        log.debug("Récupération de la pharmacie ID: {}", id);
        Pharmacie pharmacie = pharmacieRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Pharmacie", id));

        return pharmacieToResponseWithSite(pharmacie);
    }

    /**
     * Récupère les pharmacies auxquelles un utilisateur a accès.
     */
    public List<PharmacieResponse> findByUtilisateur(Long utilisateurId) {
        log.debug("Récupération des pharmacies pour l'utilisateur ID: {}", utilisateurId);

        List<DroitPharmacie> droits = droitPharmacieRepository.findByUtilisateur(utilisateurId);

        return droits.stream()
                .map(droit -> pharmacieRepository.findById(droit.getFkPharmacie()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(this::pharmacieToResponseWithSite)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une Pharmacie (domain) en PharmacieResponse (DTO) avec le nom du site.
     */
    private PharmacieResponse pharmacieToResponseWithSite(Pharmacie pharmacie) {
        if (pharmacie == null) {
            return null;
        }

        PharmacieResponse response = PharmacieResponse.builder()
                .id(pharmacie.getId())
                .fkSite(pharmacie.getFkSite())
                .designation(pharmacie.getDesignation())
                .typePharmacie(pharmacie.getTypePharmacie())
                .codeimmo(pharmacie.getCodeimmo())
                .typeHospi(pharmacie.getTypeHospi())
                .dateCreate(pharmacie.getDateCreate())
                .dateUpdate(pharmacie.getDateUpdate())
                .build();

        // Enrichir avec le nom du site
        if (pharmacie.getFkSite() != null) {
            siteRepository.findById(pharmacie.getFkSite())
                    .ifPresent(site -> response.setSiteDesignation(site.getDesignation()));
        }

        return response;
    }
}

