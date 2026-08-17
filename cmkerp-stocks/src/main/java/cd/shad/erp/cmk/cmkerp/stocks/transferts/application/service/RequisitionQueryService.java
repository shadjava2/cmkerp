package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.RequisitionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneRequisitionReportDTO;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Query Service pour la gestion des requisitions (lecture uniquement).
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class RequisitionQueryService {

    private final JdbcTemplate jdbcTemplate;

    public RequisitionQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<RequisitionResponse> REQUISITION_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return RequisitionResponse.builder()
                .id(rs.getLong("id"))
                .fkPharmacie(rs.getLong("fkPharmacie"))
                .pharmacieNom(rs.getString("pharmacieNom"))
                .fkPharmacieStock(rs.getLong("fkPharmacieStock"))
                .pharmacieStockNom(rs.getString("pharmacieStockNom"))
                .statut(rs.getString("statut"))
                .niveau(rs.getObject("niveau", Integer.class))
                .commentaire(rs.getString("commentaire"))
                .urgent(rs.getBoolean("urgent"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .peutEtreTraite(rs.getBoolean("peutEtreTraite"))
                .build();
    };

    /**
     * Récupère une page de requisitions avec filtres.
     */
    public PageResponse<RequisitionResponse> findAll(Pageable pageable, Long fkPharmacieStock, String statut, String search) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.fkPharmacie, p1.designation as pharmacieNom, " +
                "r.fkPharmacieStock, p2.designation as pharmacieStockNom, " +
                "r.statut, r.niveau, r.commentaire, r.urgent, " +
                "r.datecreate, r.dateupdate, r.usercreateid, r.userupdateid, " +
                "CASE WHEN EXISTS (SELECT 1 FROM transferts_stock ts WHERE ts.fkRequisition = r.id AND ts.statut NOT IN ('ANNULEE')) THEN 0 ELSE 1 END as peutEtreTraite " +
                "FROM requisitions r " +
                "LEFT JOIN pharmacies p1 ON r.fkPharmacie = p1.id " +
                "LEFT JOIN pharmacies p2 ON r.fkPharmacieStock = p2.id " +
                "WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (fkPharmacieStock != null) {
            sql.append(" AND r.fkPharmacieStock = ?");
            params.add(fkPharmacieStock);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND r.statut = ?");
            params.add(statut);
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (LOWER(p1.designation) LIKE LOWER(?) OR LOWER(p2.designation) LIKE LOWER(?) OR CAST(r.id AS CHAR) LIKE ?)");
            String searchPattern = "%" + search.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        sql.append(" ORDER BY r.datecreate DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<RequisitionResponse> requisitions = jdbcTemplate.query(sql.toString(), REQUISITION_MAPPER, params.toArray());

        // Compter le total
        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM requisitions r " +
                "LEFT JOIN pharmacies p1 ON r.fkPharmacie = p1.id " +
                "LEFT JOIN pharmacies p2 ON r.fkPharmacieStock = p2.id " +
                "WHERE 1=1");
        List<Object> countParams = new ArrayList<>();

        if (fkPharmacieStock != null) {
            countSql.append(" AND r.fkPharmacieStock = ?");
            countParams.add(fkPharmacieStock);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            countSql.append(" AND r.statut = ?");
            countParams.add(statut);
        }

        if (search != null && !search.trim().isEmpty()) {
            countSql.append(" AND (LOWER(p1.designation) LIKE LOWER(?) OR LOWER(p2.designation) LIKE LOWER(?) OR CAST(r.id AS CHAR) LIKE ?)");
            String searchPattern = "%" + search.trim() + "%";
            countParams.add(searchPattern);
            countParams.add(searchPattern);
            countParams.add(searchPattern);
        }

        long totalElements = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countParams.toArray());

        return PageResponse.<RequisitionResponse>builder()
                .content(requisitions)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .hasNext(pageable.getPageNumber() < (int) Math.ceil((double) totalElements / pageable.getPageSize()) - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .build();
    }

    /**
     * Récupère une requête par son ID.
     */
    public RequisitionResponse findById(Long id) {
        String sql = "SELECT r.id, r.fkPharmacie, p1.designation as pharmacieNom, " +
                "r.fkPharmacieStock, p2.designation as pharmacieStockNom, " +
                "r.statut, r.niveau, r.commentaire, r.urgent, " +
                "r.datecreate, r.dateupdate, r.usercreateid, r.userupdateid, " +
                "CASE WHEN EXISTS (SELECT 1 FROM transferts_stock ts WHERE ts.fkRequisition = r.id AND ts.statut NOT IN ('ANNULEE')) THEN 0 ELSE 1 END as peutEtreTraite " +
                "FROM requisitions r " +
                "LEFT JOIN pharmacies p1 ON r.fkPharmacie = p1.id " +
                "LEFT JOIN pharmacies p2 ON r.fkPharmacieStock = p2.id " +
                "WHERE r.id = ?";

        List<RequisitionResponse> results = jdbcTemplate.query(sql, REQUISITION_MAPPER, id);
        if (results.isEmpty()) {
            throw NotFoundException.entity("Requisition", id);
        }
        return results.get(0);
    }

    /**
     * Récupère les lignes d'une requisition pour le rapport.
     */
    public List<LigneRequisitionReportDTO> findLignesByRequisitionId(Long requisitionId) {
        // Récupérer les lignes avec les prix depuis la table produits
        String sql = "SELECT lr.id, lr.fkStock, " +
                "p.nomcommercial as nomCommercial, p.nomscientifique as nomScientifique, " +
                "f.designation as forme, d.designation as dosage, c.designation as conditionnement, " +
                "lr.quantite, " +
                "COALESCE(s.qte, 0) as quantiteEnStock, " +
                "COALESCE(p.prixachat, 0) as prixUnitaire " +
                "FROM lignes_requisitions lr " +
                "LEFT JOIN stock_produits s ON lr.fkStock = s.id " +
                "LEFT JOIN produits p ON s.fkProduits = p.id " +
                "LEFT JOIN formes f ON p.fkForme = f.id " +
                "LEFT JOIN dosages d ON p.fkDosage = d.id " +
                "LEFT JOIN conditionnements c ON p.fkConditionnement = c.id " +
                "WHERE lr.fkRequisition = ? " +
                "ORDER BY lr.datecreate ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Float quantite = rs.getObject("quantite", Float.class);
            // Récupérer le prix unitaire depuis produits.prixachat
            BigDecimal prixUnitaireDecimal = rs.getBigDecimal("prixUnitaire");
            Float prixUnitaire = null;
            Float total = null;

            // Convertir BigDecimal en Float si disponible
            if (prixUnitaireDecimal != null && prixUnitaireDecimal.compareTo(BigDecimal.ZERO) > 0) {
                prixUnitaire = prixUnitaireDecimal.floatValue();

                // Calculer le total si on a quantité et prix unitaire
                if (quantite != null && quantite > 0) {
                    total = quantite * prixUnitaire;
                }
            }

            return LigneRequisitionReportDTO.builder()
                    .id(rs.getLong("id"))
                    .fkStock(rs.getLong("fkStock"))
                    .nomCommercial(rs.getString("nomCommercial"))
                    .nomScientifique(rs.getString("nomScientifique"))
                    .forme(rs.getString("forme"))
                    .dosage(rs.getString("dosage"))
                    .conditionnement(rs.getString("conditionnement"))
                    .quantite(quantite)
                    .quantiteEnStock(rs.getObject("quantiteEnStock", Float.class))
                    .prixUnitaire(prixUnitaire)
                    .total(total)
                    .build();
        }, requisitionId);
    }
}

