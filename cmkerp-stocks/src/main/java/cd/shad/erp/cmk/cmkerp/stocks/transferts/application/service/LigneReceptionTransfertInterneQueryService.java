package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.LigneReceptionTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.LigneReceptionTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query Service pour la gestion des lignes de réception de transfert interne (lecture uniquement).
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class LigneReceptionTransfertInterneQueryService {

    private final LigneReceptionTransfertInterneRepository ligneReceptionTransfertInterneRepository;
    private final LigneReceptionTransfertInterneMapper ligneReceptionTransfertInterneMapper;
    private final JdbcTemplate jdbcTemplate;

    public LigneReceptionTransfertInterneQueryService(
            LigneReceptionTransfertInterneRepository ligneReceptionTransfertInterneRepository,
            LigneReceptionTransfertInterneMapper ligneReceptionTransfertInterneMapper,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.ligneReceptionTransfertInterneRepository = ligneReceptionTransfertInterneRepository;
        this.ligneReceptionTransfertInterneMapper = ligneReceptionTransfertInterneMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Récupère toutes les lignes d'une réception de transfert interne avec toutes les informations produit.
     */
    public List<LigneReceptionTransfertInterneResponse> findByFkReceptionTransfertInterne(Long fkReceptionTransfertInterne) {
        // Récupérer d'abord le fkPharmacieDestination pour la réception
        String getPharmacieDestinationSql = """
            SELECT ti.fkPharmacieDestination
            FROM reception_transfert_interne rti
            INNER JOIN transfert_interne ti ON rti.fkTransfertInterne = ti.id
            WHERE rti.id = ?
            """;

        Long fkPharmacieDestination;
        try {
            fkPharmacieDestination = jdbcTemplate.queryForObject(getPharmacieDestinationSql, Long.class, fkReceptionTransfertInterne);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de fkPharmacieDestination pour réception ID: {}", fkReceptionTransfertInterne, e);
            throw new NotFoundException("Réception introuvable ou transfert interne introuvable");
        }

        // Récupérer les lignes avec toutes les informations produit et stock
        String sql = """
            SELECT
                lrti.id,
                lrti.fkReceptionStock as fkReceptionTransfertInterne,
                lrti.fkStock,
                lrti.fkAlertePeremption,
                lrti.quantiteTransferee,
                lrti.quantite,
                lrti.datecreate,
                lrti.dateupdate,
                lrti.usercreateid,
                lrti.userupdateid,
                p.nomcommercial as nomCommercial,
                p.nomscientifique as nomScientifique,
                f.designation as forme,
                d.designation as dosage,
                c.designation as conditionnement,
                COALESCE(pa.peremption, NULL) as peremption,
                COALESCE(sd.qte, 0) as stockActuel
            FROM lignes_reception_transfert_interne lrti
            INNER JOIN stock_produits s ON lrti.fkStock = s.id
            INNER JOIN produits p ON s.fkProduits = p.id
            LEFT JOIN formes f ON p.fkForme = f.id
            LEFT JOIN dosages d ON p.fkDosage = d.id
            LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
            LEFT JOIN (
                SELECT
                    fkStock,
                    GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
                FROM perimable_alerte_stock
                WHERE notifactif = TRUE
                GROUP BY fkStock
            ) pa ON s.id = pa.fkStock
            LEFT JOIN stock_produits sd ON s.fkProduits = sd.fkProduits AND sd.fkPharmacies = ?
            WHERE lrti.fkReceptionStock = ?
            ORDER BY lrti.datecreate ASC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            java.sql.Timestamp dateCreateTs = rs.getTimestamp("datecreate");
            java.sql.Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

            return LigneReceptionTransfertInterneResponse.builder()
                    .id(rs.getLong("id"))
                    .fkReceptionTransfertInterne(rs.getLong("fkReceptionTransfertInterne"))
                    .fkStock(rs.getLong("fkStock"))
                    .fkAlertePeremption(rs.getObject("fkAlertePeremption", Long.class))
                    .nomCommercial(rs.getString("nomCommercial"))
                    .nomScientifique(rs.getString("nomScientifique"))
                    .forme(rs.getString("forme"))
                    .dosage(rs.getString("dosage"))
                    .conditionnement(rs.getString("conditionnement"))
                    .peremption(rs.getString("peremption"))
                    .quantiteTransferee(rs.getObject("quantiteTransferee", Float.class))
                    .quantite(rs.getObject("quantite", Float.class))
                    .stockActuel(rs.getObject("stockActuel", Float.class))
                    .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                    .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                    .userCreatedId(rs.getObject("usercreateid", Long.class))
                    .userUpdatedId(rs.getObject("userupdateid", Long.class))
                    .build();
        }, fkPharmacieDestination, fkReceptionTransfertInterne);
    }

    /**
     * Récupère une ligne par son ID avec toutes les informations produit.
     */
    public LigneReceptionTransfertInterneResponse findById(Long id) {
        LigneReceptionTransfertInterne ligne = ligneReceptionTransfertInterneRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("LigneReceptionTransfertInterne", id));

        // Récupérer d'abord le fkPharmacieDestination pour la réception
        String getPharmacieDestinationSql = """
            SELECT ti.fkPharmacieDestination
            FROM lignes_reception_transfert_interne lrti
            INNER JOIN reception_transfert_interne rti ON lrti.fkReceptionStock = rti.id
            INNER JOIN transfert_interne ti ON rti.fkTransfertInterne = ti.id
            WHERE lrti.id = ?
            """;

        Long fkPharmacieDestination;
        try {
            fkPharmacieDestination = jdbcTemplate.queryForObject(getPharmacieDestinationSql, Long.class, id);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de fkPharmacieDestination pour ligne ID: {}", id, e);
            throw NotFoundException.entity("LigneReceptionTransfertInterne", id);
        }

        // Récupérer toutes les informations produit
        String sql = """
            SELECT
                p.nomcommercial as nomCommercial,
                p.nomscientifique as nomScientifique,
                f.designation as forme,
                d.designation as dosage,
                c.designation as conditionnement,
                COALESCE(pa.peremption, NULL) as peremption,
                COALESCE(sd.qte, 0) as stockActuel
            FROM stock_produits s
            INNER JOIN produits p ON s.fkProduits = p.id
            LEFT JOIN formes f ON p.fkForme = f.id
            LEFT JOIN dosages d ON p.fkDosage = d.id
            LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
            LEFT JOIN (
                SELECT
                    fkStock,
                    GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
                FROM perimable_alerte_stock
                WHERE notifactif = TRUE
                GROUP BY fkStock
            ) pa ON s.id = pa.fkStock
            LEFT JOIN stock_produits sd ON s.fkProduits = sd.fkProduits AND sd.fkPharmacies = ?
            WHERE s.id = ?
            """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                return ligneReceptionTransfertInterneMapper.toResponse(
                        ligne,
                        rs.getString("nomCommercial"),
                        rs.getString("nomScientifique"),
                        rs.getString("forme"),
                        rs.getString("dosage"),
                        rs.getString("conditionnement"),
                        rs.getString("peremption"),
                        rs.getObject("stockActuel", Float.class)
                );
            }, fkPharmacieDestination, ligne.getFkStock());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la ligne ID: {} - Erreur: {}", id, e.getMessage(), e);
            throw NotFoundException.entity("LigneReceptionTransfertInterne", id);
        }
    }
}

