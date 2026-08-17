package cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.PharmacieRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model.Pharmacie;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository.PharmacieRepository;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.service.PharmacieDomainService;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.DroitPharmacie;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.DroitPharmacieRepository;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.repository.SiteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service pour la gestion des pharmacies.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PharmacieApplicationService {

    private final PharmacieRepository pharmacieRepository;
    private final SiteRepository siteRepository;
    private final DroitPharmacieRepository droitPharmacieRepository;
    private final PharmacieDomainService pharmacieDomainService;

    @Transactional(readOnly = true)
    public List<PharmacieResponse> findAll() {
        log.debug("Récupération de toutes les pharmacies");
        return pharmacieRepository.findAll().stream()
                .map(this::pharmacieToResponseWithSite)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PharmacieResponse findById(Long id) {
        log.debug("Récupération de la pharmacie ID: {}", id);
        Pharmacie pharmacie = pharmacieRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Pharmacie", id));

        return pharmacieToResponseWithSite(pharmacie);
    }

    @Transactional(readOnly = true)
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

    public PharmacieResponse create(PharmacieRequest request, Long currentUserId) {
        log.debug("Création d'une nouvelle pharmacie: {}", request.getDesignation());

        // Vérifier que le site existe
        siteRepository.findById(request.getFkSite())
                .orElseThrow(() -> NotFoundException.entity("Site", request.getFkSite()));

        // Validation métier via Domain Service
        pharmacieDomainService.validerCreationPharmacie(request.getCodeimmo(), request.getFkSite());

        // Créer l'agrégat Pharmacie
        Pharmacie pharmacie = Pharmacie.builder()
                .fkSite(request.getFkSite())
                .designation(request.getDesignation())
                .typePharmacie(request.getTypePharmacie())
                .codeimmo(request.getCodeimmo())
                .typeHospi(request.getTypeHospi())
                .userCreatedId(currentUserId)
                .dateCreate(LocalDateTime.now())
                .build();

        // Utiliser les méthodes métier de l'agrégat
        pharmacie.associerASite(request.getFkSite());
        if (request.getDesignation() != null) {
            pharmacie.changerDesignation(request.getDesignation());
        }
        if (request.getCodeimmo() != null) {
            pharmacie.changerCodeImmo(request.getCodeimmo());
        }

        // Sauvegarder via le repository
        int rows = pharmacieRepository.save(pharmacie);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de la pharmacie");
        }

        // Récupérer la pharmacie créée avec son ID
        Pharmacie created = request.getCodeimmo() != null && !request.getCodeimmo().isEmpty()
                ? pharmacieRepository.findByCodeImmo(request.getCodeimmo())
                        .orElseThrow(() -> new BusinessException("Erreur lors de la récupération de la pharmacie créée"))
                : pharmacieRepository.findBySite(request.getFkSite()).stream()
                        .filter(p -> p.getDesignation().equals(request.getDesignation()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("Erreur lors de la récupération de la pharmacie créée"));

        log.info("Pharmacie créée avec succès: ID={}, designation={}", created.getId(), created.getDesignation());
        return pharmacieToResponseWithSite(created);
    }

    public PharmacieResponse update(Long id, PharmacieRequest request, Long currentUserId) {
        log.debug("Mise à jour de la pharmacie ID: {}", id);

        Pharmacie pharmacie = pharmacieRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Pharmacie", id));

        // Vérifier que le site existe si modifié
        if (request.getFkSite() != null && !request.getFkSite().equals(pharmacie.getFkSite())) {
            siteRepository.findById(request.getFkSite())
                    .orElseThrow(() -> NotFoundException.entity("Site", request.getFkSite()));
            pharmacie.associerASite(request.getFkSite());
        }

        // Validation métier via Domain Service si le codeImmo change
        if (request.getCodeimmo() != null && !request.getCodeimmo().equals(pharmacie.getCodeimmo())) {
            pharmacieDomainService.validerModificationPharmacie(pharmacie, request.getCodeimmo());
            pharmacie.changerCodeImmo(request.getCodeimmo());
        }

        if (request.getDesignation() != null) {
            pharmacie.changerDesignation(request.getDesignation());
        }
        if (request.getTypePharmacie() != null) {
            pharmacie.changerTypePharmacie(request.getTypePharmacie());
        }

        pharmacie.setUserUpdatedId(currentUserId);
        pharmacie.setDateUpdate(LocalDateTime.now());

        // Sauvegarder via le repository
        int rows = pharmacieRepository.update(pharmacie);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de la pharmacie");
        }

        log.info("Pharmacie mise à jour avec succès: ID={}", pharmacie.getId());
        return pharmacieToResponseWithSite(pharmacie);
    }

    public void deleteById(Long id) {
        log.debug("Suppression de la pharmacie ID: {}", id);

        pharmacieRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Pharmacie", id));

        int rows = pharmacieRepository.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("Échec de la suppression de la pharmacie");
        }

        log.info("Pharmacie supprimée avec succès: ID={}", id);
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

