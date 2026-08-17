package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.ReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.ReceptionTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.ReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.ReceptionTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.TransfertInterneRepository;
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
 * Query Service pour la gestion des réceptions de transferts internes (lecture uniquement).
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class ReceptionTransfertInterneQueryService {

    private final ReceptionTransfertInterneRepository receptionTransfertInterneRepository;
    private final TransfertInterneRepository transfertInterneRepository;
    private final ReceptionTransfertInterneMapper receptionTransfertInterneMapper;
    private final JdbcTemplate jdbcTemplate;

    public ReceptionTransfertInterneQueryService(
            ReceptionTransfertInterneRepository receptionTransfertInterneRepository,
            TransfertInterneRepository transfertInterneRepository,
            ReceptionTransfertInterneMapper receptionTransfertInterneMapper,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.receptionTransfertInterneRepository = receptionTransfertInterneRepository;
        this.transfertInterneRepository = transfertInterneRepository;
        this.receptionTransfertInterneMapper = receptionTransfertInterneMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Récupère une page de réceptions de transferts internes avec filtres.
     * Filtre par défaut : fkPharmacieDestination = pharmacie actuelle.
     * IMPORTANT : Seules les réceptions avec statut ANNULEE ou RECEPTIONNEE sont retournées.
     * Le repository gère automatiquement ce filtrage.
     */
    public PageResponse<ReceptionTransfertInterneResponse> findAll(Pageable pageable, Long fkPharmacieDestination, String statut) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        // Le repository filtre automatiquement sur ANNULEE et RECEPTIONNEE uniquement
        List<ReceptionTransfertInterne> receptions = receptionTransfertInterneRepository.findAll(offset, limit, fkPharmacieDestination, statut);
        long totalElements = receptionTransfertInterneRepository.count(fkPharmacieDestination, statut);

        // Récupérer les informations du transfert interne associé
        List<ReceptionTransfertInterneResponse> responses = receptions.stream()
                .map(reception -> {
                    TransfertInterne transfert = transfertInterneRepository.findById(reception.getFkTransfertInterne())
                            .orElse(null);

                    if (transfert == null) {
                        log.warn("Transfert interne introuvable pour réception ID: {}", reception.getId());
                        return null;
                    }

                    String transfertInterneNumero = String.valueOf(transfert.getId());
                    String pharmacieSourceNom = getPharmacieNom(transfert.getFkPharmacieSource());
                    String pharmacieDestinationNom = getPharmacieNom(transfert.getFkPharmacieDestination());

                    return receptionTransfertInterneMapper.toResponse(
                            reception,
                            transfertInterneNumero,
                            transfert.getFkPharmacieSource(),
                            pharmacieSourceNom,
                            transfert.getFkPharmacieDestination(),
                            pharmacieDestinationNom
                    );
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());

        return PageResponse.<ReceptionTransfertInterneResponse>builder()
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
     * Récupère les transferts internes à réceptionner (statut TRANSFEREE et fkPharmacieDestination = pharmacie actuelle).
     */
    public PageResponse<ReceptionTransfertInterneResponse> findTransfertsInternesARecevoir(Pageable pageable, Long fkPharmacieDestination) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        // Récupérer les transferts internes avec statut TRANSFEREE et fkPharmacieDestination = pharmacie actuelle
        // Logique simple et robuste : afficher tous les transferts TRANSFEREE pour le service sélectionné
        String sql = """
            SELECT ti.id, ti.fkPharmacieSource, ti.fkPharmacieDestination, ti.statut, ti.commentaire, ti.perime,
                   ti.datecreate, ti.dateupdate, ti.usercreateid, ti.userupdateid
            FROM transfert_interne ti
            WHERE ti.statut = 'TRANSFEREE'
              AND ti.fkPharmacieDestination = ?
            ORDER BY ti.datecreate DESC
            LIMIT ? OFFSET ?
            """;

        String countSql = """
            SELECT COUNT(*)
            FROM transfert_interne ti
            WHERE ti.statut = 'TRANSFEREE'
              AND ti.fkPharmacieDestination = ?
            """;

        // Utiliser un RowMapper pour mapper correctement les transferts
        org.springframework.jdbc.core.RowMapper<TransfertInterne> rowMapper = (rs, rowNum) -> {
            java.sql.Timestamp dateCreateTs = rs.getTimestamp("datecreate");
            java.sql.Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

            return TransfertInterne.builder()
                    .id(rs.getLong("id"))
                    .fkPharmacieSource(rs.getLong("fkPharmacieSource"))
                    .fkPharmacieDestination(rs.getLong("fkPharmacieDestination"))
                    .statut(convertStatutFromDatabase(rs.getString("statut")))
                    .commentaire(rs.getString("commentaire"))
                    .perime(rs.getObject("perime", Boolean.class))
                    .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                    .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                    .userCreatedId(rs.getObject("usercreateid", Long.class))
                    .userUpdatedId(rs.getObject("userupdateid", Long.class))
                    .build();
        };

        List<TransfertInterne> transferts = jdbcTemplate.query(sql, rowMapper, fkPharmacieDestination, limit, offset);

        Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class, fkPharmacieDestination);
        long totalElements = totalCount != null ? totalCount : 0L;

        // Convertir en ReceptionTransfertInterneResponse (sans réception créée)
        List<ReceptionTransfertInterneResponse> responses = transferts.stream()
                .map(transfert -> {
                    String transfertInterneNumero = String.valueOf(transfert.getId());
                    String pharmacieSourceNom = getPharmacieNom(transfert.getFkPharmacieSource());
                    String pharmacieDestinationNom = getPharmacieNom(transfert.getFkPharmacieDestination());

                    // Calculer statutTransfert : "transfert périmé" si perime=true, sinon "transfert stock"
                    Boolean perime = transfert.getPerime();
                    String statutTransfert = (perime != null && perime) ? "transfert périmé" : "transfert stock";

                    // Créer une réponse "virtuelle" pour le transfert interne à réceptionner
                    // Le statut affiché est le statut réel du transfert_interne (TRANSFEREE)
                    return ReceptionTransfertInterneResponse.builder()
                            .id(null) // Pas encore de réception créée
                            .fkTransfertInterne(transfert.getId())
                            .transfertInterneNumero(transfertInterneNumero)
                            .fkPharmacieSource(transfert.getFkPharmacieSource())
                            .pharmacieSourceNom(pharmacieSourceNom)
                            .fkPharmacieDestination(transfert.getFkPharmacieDestination())
                            .pharmacieDestinationNom(pharmacieDestinationNom)
                            .statut(transfert.getStatut().getDbValue()) // Statut réel depuis transfert_interne
                            .perime(perime)
                            .statutTransfert(statutTransfert)
                            .dateCreate(transfert.getDateCreate())
                            .dateUpdate(transfert.getDateUpdate())
                            .userCreatedId(transfert.getUserCreatedId())
                            .userUpdatedId(transfert.getUserUpdatedId())
                            .peutEtreAnnule(true) // Peut être annulé car pas encore créé
                            .build();
                })
                .collect(Collectors.toList());

        return PageResponse.<ReceptionTransfertInterneResponse>builder()
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
     * Récupère une réception de transfert interne par son ID.
     */
    public ReceptionTransfertInterneResponse findById(Long id) {
        ReceptionTransfertInterne reception = receptionTransfertInterneRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("ReceptionTransfertInterne", id));

        TransfertInterne transfert = transfertInterneRepository.findById(reception.getFkTransfertInterne())
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", reception.getFkTransfertInterne()));

        String transfertInterneNumero = String.valueOf(transfert.getId());
        String pharmacieSourceNom = getPharmacieNom(transfert.getFkPharmacieSource());
        String pharmacieDestinationNom = getPharmacieNom(transfert.getFkPharmacieDestination());

        return receptionTransfertInterneMapper.toResponse(
                reception,
                transfertInterneNumero,
                transfert.getFkPharmacieSource(),
                pharmacieSourceNom,
                transfert.getFkPharmacieDestination(),
                pharmacieDestinationNom
        );
    }

    /**
     * Récupère une réception de transfert interne par fkTransfertInterne.
     */
    public ReceptionTransfertInterneResponse findByFkTransfertInterne(Long fkTransfertInterne) {
        ReceptionTransfertInterne reception = receptionTransfertInterneRepository.findByFkTransfertInterne(fkTransfertInterne)
                .orElseThrow(() -> NotFoundException.entity("ReceptionTransfertInterne", fkTransfertInterne));

        TransfertInterne transfert = transfertInterneRepository.findById(fkTransfertInterne)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", fkTransfertInterne));

        String transfertInterneNumero = String.valueOf(transfert.getId());
        String pharmacieSourceNom = getPharmacieNom(transfert.getFkPharmacieSource());
        String pharmacieDestinationNom = getPharmacieNom(transfert.getFkPharmacieDestination());

        return receptionTransfertInterneMapper.toResponse(
                reception,
                transfertInterneNumero,
                transfert.getFkPharmacieSource(),
                pharmacieSourceNom,
                transfert.getFkPharmacieDestination(),
                pharmacieDestinationNom
        );
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

    private static TransfertInterne.StatutTransfertInterne convertStatutFromDatabase(String statut) {
        return TransfertInterne.StatutTransfertInterne.fromDbValue(statut);
    }
}

