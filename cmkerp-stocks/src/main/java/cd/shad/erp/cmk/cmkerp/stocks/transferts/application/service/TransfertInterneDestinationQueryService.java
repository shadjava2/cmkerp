package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query Service pour récupérer les pharmacies destinations éligibles pour un transfert interne.
 *
 * <p>Retourne toutes les pharmacies en base, à l'exception de la pharmacie source.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class TransfertInterneDestinationQueryService {

    private final JdbcTemplate jdbcTemplate;

    public TransfertInterneDestinationQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<PharmacieResponse> PHARMACIE_MAPPER = (rs, rowNum) -> {
        return PharmacieResponse.builder()
                .id(rs.getLong("id"))
                .designation(rs.getString("designation"))
                .typePharmacie(rs.getString("typepharmacie"))
                .fkSite(rs.getObject("fkSite", Long.class))
                .build();
    };

    /**
     * Récupère toutes les pharmacies pouvant servir de destination (hors pharmacie source).
     */
    public List<PharmacieResponse> findDestinationsEligibles(Long sourcePharmacieId) {
        log.debug("Récupération de toutes les pharmacies destinations pour source: {}", sourcePharmacieId);

        if (sourcePharmacieId == null) {
            return List.of();
        }

        String sql = """
            SELECT p.id, p.designation, p.typepharmacie, p.fkSite
            FROM pharmacies p
            WHERE p.id != ?
            ORDER BY p.designation ASC
            """;

        List<PharmacieResponse> pharmacies = jdbcTemplate.query(sql, PHARMACIE_MAPPER, sourcePharmacieId);

        log.debug("Trouvé {} pharmacies destinations pour la pharmacie source {}", pharmacies.size(), sourcePharmacieId);
        return pharmacies;
    }
}
