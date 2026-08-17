package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.InventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.mapper.InventaireMapper;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.Inventaire;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository.InventaireRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des inventaires (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InventaireQueryService {

    private final InventaireRepository inventaireRepository;
    private final InventaireMapper inventaireMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Récupère une page d'inventaires avec filtres.
     */
    public PageResponse<InventaireResponse> findAll(Pageable pageable, Long fkPharmacie, String statut, String typeinventaire,
            LocalDate dateFrom, LocalDate dateTo, String searchText) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<Inventaire> inventaires = inventaireRepository.findAll(offset, limit, fkPharmacie, statut, typeinventaire, dateFrom, dateTo, searchText);
        long totalElements = inventaireRepository.count(fkPharmacie, statut, typeinventaire, dateFrom, dateTo, searchText);

        // Récupérer les désignations via JOINs
        List<InventaireResponse> responses = inventaires.stream()
                .map(inventaire -> {
                    String pharmacieNom = getPharmacieNom(inventaire.getFkPharmacie());
                    return inventaireMapper.toResponse(inventaire, pharmacieNom);
                })
                .collect(Collectors.toList());

        int pageSize = pageable.getPageSize();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        if (totalElements == 0) {
            totalPages = 0;
        }

        return PageResponse.<InventaireResponse>builder()
                .content(responses)
                .page(pageable.getPageNumber())
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(pageable.getPageNumber() < totalPages - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .build();
    }

    /**
     * Récupère un inventaire par son ID.
     */
    public InventaireResponse findById(Long id) {
        Inventaire inventaire = inventaireRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Inventaire", id));

        String pharmacieNom = getPharmacieNom(inventaire.getFkPharmacie());

        return inventaireMapper.toResponse(inventaire, pharmacieNom);
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

