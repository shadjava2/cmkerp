package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper.VenteMapper;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.Vente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.VenteRepository;
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
 * Query Service pour la gestion des ventes (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VenteQueryService {

    private final VenteRepository venteRepository;
    private final VenteMapper venteMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Récupère une page de ventes avec filtres.
     */
    public PageResponse<VenteResponse> findAll(Pageable pageable, Long fkPharmacie, String statut, Long fkPatient,
            LocalDate dateFrom, LocalDate dateTo, String searchText) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<Vente> ventes = venteRepository.findAll(offset, limit, fkPharmacie, statut, fkPatient, dateFrom, dateTo, searchText);
        long totalElements = venteRepository.count(fkPharmacie, statut, fkPatient, dateFrom, dateTo, searchText);

        // Récupérer les désignations via JOINs
        List<VenteResponse> responses = ventes.stream()
                .map(vente -> {
                    String pharmacieNom = getPharmacieNom(vente.getFkPharmacie());
                    return venteMapper.toResponse(vente, pharmacieNom);
                })
                .collect(Collectors.toList());

        return PageResponse.<VenteResponse>builder()
                .content(responses)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .hasNext(pageable.getPageNumber() < (int) Math.ceil((double) totalElements / pageable.getPageSize()) - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .build();
    }

    /**
     * Récupère une vente par son ID.
     */
    public VenteResponse findById(Long id) {
        Vente vente = venteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Vente", id));

        String pharmacieNom = getPharmacieNom(vente.getFkPharmacie());

        return venteMapper.toResponse(vente, pharmacieNom);
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

