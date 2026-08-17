package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertReportDTO;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Query Service pour la gestion des transferts de stock (lecture uniquement).
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class TransfertStockQueryService {

    private final JdbcTemplate jdbcTemplate;

    public TransfertStockQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<TransfertStockResponse> TRANSFERT_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return TransfertStockResponse.builder()
                .id(rs.getLong("id"))
                .fkRequisition(rs.getLong("fkRequisition"))
                .requisitionNumero(rs.getObject("requisitionNumero", Long.class))
                .statut(rs.getString("statut"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .pharmacieDemandeurNom(rs.getString("pharmacieDemandeurNom"))
                .build();
    };

    static final RowMapper<LigneTransfertStockResponse> LIGNE_TRANSFERT_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return LigneTransfertStockResponse.builder()
                .id(rs.getLong("id"))
                .fkTransfertStock(rs.getLong("fkTransfertStock"))
                .fkStock(rs.getObject("fkStock") != null ? rs.getLong("fkStock") : null)
                .stockNomCommercial(rs.getString("stockNomCommercial"))
                .stockNomScientifique(rs.getString("stockNomScientifique"))
                .stockForme(rs.getString("stockForme"))
                .stockDosage(rs.getString("stockDosage"))
                .stockConditionnement(rs.getString("stockConditionnement"))
                .quantiteDemandee(readDouble(rs, "quantiteDemandee"))
                .quantite(readDouble(rs, "quantite"))
                .quantiteEnStock(readDouble(rs, "quantiteEnStock"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid") != null ? rs.getLong("usercreateid") : null)
                .userUpdatedId(rs.getObject("userupdateid") != null ? rs.getLong("userupdateid") : null)
                .build();
    };

    /**
     * Récupère une page de transferts avec filtres.
     */
    public PageResponse<TransfertStockResponse> findAll(Pageable pageable, Long fkPharmacieStock, String statut, String search) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        StringBuilder sql = new StringBuilder(
                "SELECT ts.id, ts.fkRequisition, r.id as requisitionNumero, " +
                "ts.statut, ts.datecreate, ts.dateupdate, ts.usercreateid, ts.userupdateid, " +
                "p.designation as pharmacieDemandeurNom " +
                "FROM transferts_stock ts " +
                "LEFT JOIN requisitions r ON ts.fkRequisition = r.id " +
                "LEFT JOIN pharmacies p ON r.fkPharmacie = p.id " +
                "WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (fkPharmacieStock != null) {
            sql.append(" AND r.fkPharmacieStock = ?");
            params.add(fkPharmacieStock);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND ts.statut = ?");
            params.add(statut);
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (LOWER(p.designation) LIKE LOWER(?) OR CAST(ts.id AS CHAR) LIKE ?)");
            String searchPattern = "%" + search.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        sql.append(" ORDER BY ts.datecreate DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<TransfertStockResponse> transferts = jdbcTemplate.query(sql.toString(), TRANSFERT_MAPPER, params.toArray());

        // Compter le total
        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM transferts_stock ts " +
                "LEFT JOIN requisitions r ON ts.fkRequisition = r.id " +
                "LEFT JOIN pharmacies p ON r.fkPharmacie = p.id " +
                "WHERE 1=1");
        List<Object> countParams = new ArrayList<>();

        if (fkPharmacieStock != null) {
            countSql.append(" AND r.fkPharmacieStock = ?");
            countParams.add(fkPharmacieStock);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            countSql.append(" AND ts.statut = ?");
            countParams.add(statut);
        }

        if (search != null && !search.trim().isEmpty()) {
            countSql.append(" AND (LOWER(p.designation) LIKE LOWER(?) OR CAST(ts.id AS CHAR) LIKE ?)");
            String searchPattern = "%" + search.trim() + "%";
            countParams.add(searchPattern);
            countParams.add(searchPattern);
        }

        long totalElements = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countParams.toArray());

        return PageResponse.<TransfertStockResponse>builder()
                .content(transferts)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .hasNext(pageable.getPageNumber() < (int) Math.ceil((double) totalElements / pageable.getPageSize()) - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .build();
    }

    /**
     * Récupère un transfert par son ID.
     */
    public TransfertStockResponse findById(Long id) {
        String sql = "SELECT ts.id, ts.fkRequisition, r.id as requisitionNumero, " +
                "ts.statut, ts.datecreate, ts.dateupdate, ts.usercreateid, ts.userupdateid, " +
                "p.designation as pharmacieDemandeurNom " +
                "FROM transferts_stock ts " +
                "LEFT JOIN requisitions r ON ts.fkRequisition = r.id " +
                "LEFT JOIN pharmacies p ON r.fkPharmacie = p.id " +
                "WHERE ts.id = ?";

        List<TransfertStockResponse> results = jdbcTemplate.query(sql, TRANSFERT_MAPPER, id);
        if (results.isEmpty()) {
            throw NotFoundException.entity("TransfertStock", id);
        }
        return results.get(0);
    }

    /**
     * Récupère les lignes d'un transfert.
     * <p>Utilise la même requête que le rapport PDF pour éviter les écarts d'affichage.
     */
    public List<LigneTransfertStockResponse> findLignesByTransfertId(Long transfertId) {
        return findLignesByTransfertIdForReport(transfertId).stream()
                .map(r -> LigneTransfertStockResponse.builder()
                        .id(r.getId())
                        .fkTransfertStock(transfertId)
                        .fkStock(r.getFkStock())
                        .stockNomCommercial(r.getNomCommercial())
                        .stockNomScientifique(r.getNomScientifique())
                        .stockForme(r.getForme())
                        .stockDosage(r.getDosage())
                        .stockConditionnement(r.getConditionnement())
                        .quantiteDemandee(r.getQuantiteDemandee())
                        .quantite(r.getQuantite())
                        .quantiteEnStock(r.getQuantiteEnStock())
                        .build())
                .toList();
    }

    /**
     * Récupère les lignes d'un transfert pour le rapport.
     */
    public List<LigneTransfertReportDTO> findLignesByTransfertIdForReport(Long transfertId) {
        String sql = """
                SELECT lts.id, lts.fkStock,
                       p.nomcommercial AS nomCommercial,
                       p.nomscientifique AS nomScientifique,
                       f.designation AS forme,
                       d.designation AS dosage,
                       c.designation AS conditionnement,
                       lts.quantiteDemandee,
                       lts.quantite,
                       COALESCE(s.qte, 0) AS quantiteEnStock
                FROM lignes_transferts_stock lts
                LEFT JOIN stock_produits s ON lts.fkStock = s.id
                LEFT JOIN produits p ON s.fkProduits = p.id
                LEFT JOIN formes f ON p.fkForme = f.id
                LEFT JOIN dosages d ON p.fkDosage = d.id
                LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
                WHERE lts.fkTransfertStock = ?
                ORDER BY lts.id ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> LigneTransfertReportDTO.builder()
                .id(rs.getLong("id"))
                .fkStock(rs.getObject("fkStock") != null ? rs.getLong("fkStock") : null)
                .nomCommercial(rs.getString("nomCommercial"))
                .nomScientifique(rs.getString("nomScientifique"))
                .forme(rs.getString("forme"))
                .dosage(rs.getString("dosage"))
                .conditionnement(rs.getString("conditionnement"))
                .quantiteDemandee(readDouble(rs, "quantiteDemandee"))
                .quantite(readDouble(rs, "quantite"))
                .quantiteEnStock(readDouble(rs, "quantiteEnStock"))
                .build(), transfertId);
    }

    private static Double readDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Object value = rs.getObject(column);
        if (value == null || rs.wasNull()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

