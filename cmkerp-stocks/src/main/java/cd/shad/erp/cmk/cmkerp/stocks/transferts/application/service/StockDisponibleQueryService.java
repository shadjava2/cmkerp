package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.StockDisponibleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Query Service pour la gestion des stocks disponibles (lecture uniquement).
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class StockDisponibleQueryService {

    private final JdbcTemplate jdbcTemplate;

    public StockDisponibleQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<StockDisponibleResponse> STOCK_DISPONIBLE_MAPPER = (rs, rowNum) -> {
        return StockDisponibleResponse.builder()
                .id(rs.getLong("id"))
                .nomCommercial(rs.getString("nomCommercial"))
                .nomScientifique(rs.getString("nomScientifique"))
                .forme(rs.getString("forme"))
                .dosage(rs.getString("dosage"))
                .conditionnement(rs.getString("conditionnement"))
                .quantiteEnStock(rs.getObject("quantiteEnStock", Double.class))
                .build();
    };

    /**
     * Récupère les stocks disponibles pour remplacer un produit.
     */
    public List<StockDisponibleResponse> findAll(Long fkPharmacieStock, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.id, p.nomcommercial as nomCommercial, p.nomscientifique as nomScientifique, " +
                "f.designation as forme, d.designation as dosage, c.designation as conditionnement, " +
                "COALESCE(s.qte, 0) as quantiteEnStock " +
                "FROM stock_produits s " +
                "INNER JOIN produits p ON s.fkProduits = p.id " +
                "LEFT JOIN formes f ON p.fkForme = f.id " +
                "LEFT JOIN dosages d ON p.fkDosage = d.id " +
                "LEFT JOIN conditionnements c ON p.fkConditionnement = c.id " +
                "WHERE s.fkPharmacies = ? AND COALESCE(s.qte, 0) > 0");

        List<Object> params = new ArrayList<>();
        params.add(fkPharmacieStock);

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (LOWER(p.nomcommercial) LIKE LOWER(?) OR LOWER(p.nomscientifique) LIKE LOWER(?))");
            String searchPattern = "%" + search.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        sql.append(" ORDER BY p.nomcommercial ASC LIMIT 100");

        return jdbcTemplate.query(sql.toString(), STOCK_DISPONIBLE_MAPPER, params.toArray());
    }
}

