package cd.shad.erp.cmk.cmkerp.pos.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.TransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.mapper.TransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository.TransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des transferts internes (lecture uniquement) - module POS.
 */
@Service("posTransfertInterneQueryService")
@Transactional(readOnly = true)
@Slf4j
public class TransfertInterneQueryService {

    private final TransfertInterneRepository transfertInterneRepository;
    private final TransfertInterneMapper transfertInterneMapper;
    private final JdbcTemplate jdbcTemplate;

    public TransfertInterneQueryService(
            @Qualifier("posTransfertInterneJdbcRepositoryImpl") TransfertInterneRepository transfertInterneRepository,
            @Qualifier("posTransfertInterneMapper") TransfertInterneMapper transfertInterneMapper,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.transfertInterneRepository = transfertInterneRepository;
        this.transfertInterneMapper = transfertInterneMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Récupère une page de transferts internes avec filtres.
     */
    public PageResponse<TransfertInterneResponse> findAll(Pageable pageable, Long fkPharmacieSource, Long fkPharmacieDestination,
            String statut, String searchText) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<TransfertInterne> transferts = transfertInterneRepository.findAll(offset, limit, fkPharmacieSource, fkPharmacieDestination, statut, searchText);
        long totalElements = transfertInterneRepository.count(fkPharmacieSource, fkPharmacieDestination, statut, searchText);

        // Récupérer les désignations via JOINs
        List<TransfertInterneResponse> responses = transferts.stream()
                .map(transfert -> {
                    String pharmacieSourceNom = getPharmacieNom(transfert.getFkPharmacieSource());
                    String pharmacieDestinationNom = getPharmacieNom(transfert.getFkPharmacieDestination());
                    return transfertInterneMapper.toResponse(transfert, pharmacieSourceNom, pharmacieDestinationNom);
                })
                .collect(Collectors.toList());

        return PageResponse.<TransfertInterneResponse>builder()
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
     * Récupère un transfert interne par son ID.
     */
    public TransfertInterneResponse findById(Long id) {
        TransfertInterne transfert = transfertInterneRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", id));

        String pharmacieSourceNom = getPharmacieNom(transfert.getFkPharmacieSource());
        String pharmacieDestinationNom = getPharmacieNom(transfert.getFkPharmacieDestination());

        return transfertInterneMapper.toResponse(transfert, pharmacieSourceNom, pharmacieDestinationNom);
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

