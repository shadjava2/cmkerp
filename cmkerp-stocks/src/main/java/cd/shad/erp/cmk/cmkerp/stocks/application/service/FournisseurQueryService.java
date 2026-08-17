package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FournisseurResponse;
import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Fournisseur;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.FournisseurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des fournisseurs (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FournisseurQueryService {

    private final FournisseurRepository fournisseurRepository;

    /**
     * Récupère une page de fournisseurs avec pagination.
     */
    public PageResponse<FournisseurResponse> findAll(Pageable pageable, String nom) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        List<Fournisseur> fournisseurs = fournisseurRepository.findAll(offset, size, nom);
        long totalElements = fournisseurRepository.count(nom);

        List<FournisseurResponse> content = fournisseurs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, totalElements);
    }

    /**
     * Récupère un fournisseur par son ID.
     */
    public FournisseurResponse findById(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Fournisseur", id));
        return toResponse(fournisseur);
    }

    /**
     * Récupère tous les fournisseurs sans pagination (pour combos).
     */
    public List<FournisseurResponse> findAllWithoutPagination() {
        // Récupérer tous les fournisseurs avec une limite élevée (1000 devrait suffire)
        List<Fournisseur> fournisseurs = fournisseurRepository.findAll(0, 1000, null);
        return fournisseurs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit un modèle de domaine en DTO de réponse.
     */
    private FournisseurResponse toResponse(Fournisseur fournisseur) {
        return FournisseurResponse.builder()
                .id(fournisseur.getId())
                .nom(fournisseur.getNom())
                .adresse(fournisseur.getAdresse())
                .telephone(fournisseur.getTelephone())
                .email(fournisseur.getEmail())
                .dateCreate(fournisseur.getDateCreate())
                .dateUpdate(fournisseur.getDateUpdate())
                .build();
    }
}

