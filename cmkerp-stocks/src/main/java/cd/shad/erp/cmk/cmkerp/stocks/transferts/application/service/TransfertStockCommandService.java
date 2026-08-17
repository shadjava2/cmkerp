package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateTransfertRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.ReplaceLigneTransfertRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateLigneTransfertRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateTransfertStatusRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertStockResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des transferts de stock (écriture uniquement).
 */
@Service
@Transactional
@Slf4j
public class TransfertStockCommandService {

  private final JdbcTemplate jdbcTemplate;
  private final TransfertStockQueryService transfertStockQueryService;

  public TransfertStockCommandService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
      TransfertStockQueryService transfertStockQueryService) {
    this.jdbcTemplate = jdbcTemplate;
    this.transfertStockQueryService = transfertStockQueryService;
  }

  /**
   * Crée un nouveau transfert (traite une requête). Les lignes sont automatiquement générées par le
   * trigger de la base de données.
   */
  public TransfertStockResponse create(CreateTransfertRequest request, Long currentUserId) {
    log.debug("Création d'un transfert pour la requête: {}", request.getFkRequisition());

    // Vérifier que la requête existe
    String checkRequisitionSql = "SELECT COUNT(*) FROM requisitions WHERE id = ?";
    Long count =
        jdbcTemplate.queryForObject(checkRequisitionSql, Long.class, request.getFkRequisition());
    if (count == null || count == 0) {
      throw NotFoundException.entity("Requisition", request.getFkRequisition());
    }

    // Vérifier qu'il n'existe pas déjà un transfert actif pour cette requête
    String checkTransfertSql =
        "SELECT COUNT(*) FROM transferts_stock WHERE fkRequisition = ? AND statut NOT IN ('ANNULEE')";
    Long existingCount =
        jdbcTemplate.queryForObject(checkTransfertSql, Long.class, request.getFkRequisition());
    if (existingCount != null && existingCount > 0) {
      throw new BusinessException("Un transfert actif existe déjà pour cette requête");
    }

    // Créer le transfert (le trigger créera automatiquement les lignes)
    String insertSql =
        "INSERT INTO transferts_stock (fkRequisition, statut, datecreate, usercreateid) VALUES (?, 'EN ATTENTE', ?, ?)";
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(connection -> {
      PreparedStatement ps =
          connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, request.getFkRequisition());
      ps.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
      ps.setLong(3, currentUserId);
      return ps;
    }, keyHolder);

    Long transfertId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    if (transfertId == null) {
      throw new BusinessException("Échec de la création du transfert");
    }

    // Le trigger DB crée les lignes ; si échec (ex. plusieurs lots stock), on complète en Java
    ensureLignesFromRequisition(transfertId, currentUserId);

    log.info("Transfert créé avec succès: ID: {}", transfertId);

    // Récupérer le transfert créé
    return transfertStockQueryService.findById(transfertId);
  }

  /**
   * Garantit la présence des lignes après création (complète le trigger si besoin).
   */
  private void ensureLignesFromRequisition(Long transfertId, Long currentUserId) {
    Long existing = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM lignes_transferts_stock WHERE fkTransfertStock = ?",
        Long.class, transfertId);
    if (existing != null && existing > 0) {
      return;
    }

    log.warn("Trigger lignes absent pour transfert {} — insertion de secours", transfertId);
    int inserted = jdbcTemplate.update(
        """
        INSERT INTO lignes_transferts_stock
          (fkTransfertStock, fkStock, quantite, quantiteDemandee, usercreateid, userupdateid)
        SELECT
          ts.id,
          sp.id,
          lr.quantite,
          lr.quantite,
          ?,
          ?
        FROM lignes_requisitions lr
        INNER JOIN requisitions rs ON rs.id = lr.fkRequisition
        INNER JOIN transferts_stock ts ON ts.fkRequisition = rs.id AND ts.id = ?
        INNER JOIN stock_produits sp_req ON sp_req.id = lr.fkStock
        INNER JOIN stock_produits sp ON sp.id = (
          SELECT sp2.id
          FROM stock_produits sp2
          WHERE sp2.fkProduits = sp_req.fkProduits
            AND sp2.fkPharmacies = rs.fkPharmacieStock
          ORDER BY sp2.qte DESC, sp2.id ASC
          LIMIT 1
        )
        """,
        currentUserId, currentUserId, transfertId);

    if (inserted == 0) {
      log.error("Aucune ligne générée pour le transfert {}", transfertId);
    } else {
      log.info("{} ligne(s) générée(s) en secours pour le transfert {}", inserted, transfertId);
    }
  }

  /**
   * Met à jour une ligne de transfert.
   */
  public LigneTransfertStockResponse updateLigne(Long transfertId, Long ligneId,
      UpdateLigneTransfertRequest request, Long currentUserId) {
    log.debug("Mise à jour de la ligne - transfertId: {}, ligneId: {}", transfertId, ligneId);

    // Vérifier que la ligne existe et appartient au transfert
    String checkSql =
        "SELECT COUNT(*) FROM lignes_transferts_stock WHERE id = ? AND fkTransfertStock = ?";
    Long count = jdbcTemplate.queryForObject(checkSql, Long.class, ligneId, transfertId);
    if (count == null || count == 0) {
      throw NotFoundException.entity("LigneTransfertStock", ligneId);
    }

    // Construire la requête de mise à jour dynamiquement
    StringBuilder updateSql =
        new StringBuilder("UPDATE lignes_transferts_stock SET dateupdate = ?, userupdateid = ?");
    java.util.List<Object> params = new java.util.ArrayList<>();
    params.add(java.sql.Timestamp.valueOf(LocalDateTime.now()));
    params.add(currentUserId);

    if (request.getFkStock() != null) {
      updateSql.append(", fkStock = ?");
      params.add(request.getFkStock());
    }

    if (request.getQuantite() != null) {
      updateSql.append(", quantite = ?");
      params.add(request.getQuantite());
    }

    updateSql.append(" WHERE id = ? AND fkTransfertStock = ?");
    params.add(ligneId);
    params.add(transfertId);

    int rows = jdbcTemplate.update(updateSql.toString(), params.toArray());
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour de la ligne");
    }

    // Récupérer la ligne mise à jour
    String selectSql = "SELECT lts.id, lts.fkTransfertStock, lts.fkStock, "
        + "p.nomcommercial as stockNomCommercial, p.nomscientifique as stockNomScientifique, "
        + "f.designation as stockForme, d.designation as stockDosage, c.designation as stockConditionnement, "
        + "lts.quantiteDemandee, lts.quantite, " + "COALESCE(s.qte, 0) as quantiteEnStock, "
        + "lts.datecreate, lts.dateupdate, lts.usercreateid, lts.userupdateid "
        + "FROM lignes_transferts_stock lts " + "LEFT JOIN stock_produits s ON lts.fkStock = s.id "
        + "LEFT JOIN produits p ON s.fkProduits = p.id " + "LEFT JOIN formes f ON p.fkForme = f.id "
        + "LEFT JOIN dosages d ON p.fkDosage = d.id "
        + "LEFT JOIN conditionnements c ON p.fkConditionnement = c.id " + "WHERE lts.id = ?";

    return jdbcTemplate.query(selectSql, TransfertStockQueryService.LIGNE_TRANSFERT_MAPPER, ligneId)
        .stream().findFirst()
        .orElseThrow(() -> NotFoundException.entity("LigneTransfertStock", ligneId));
  }

  /**
   * Remplace un produit dans une ligne de transfert.
   */
  public LigneTransfertStockResponse replaceLigne(Long transfertId, Long ligneId,
      ReplaceLigneTransfertRequest request, Long currentUserId) {
    log.debug(
        "Remplacement du produit dans la ligne - transfertId: {}, ligneId: {}, nouveau fkStock: {}",
        transfertId, ligneId, request.getFkStock());

    // Vérifier que la ligne existe et appartient au transfert
    String checkSql =
        "SELECT COUNT(*) FROM lignes_transferts_stock WHERE id = ? AND fkTransfertStock = ?";
    Long count = jdbcTemplate.queryForObject(checkSql, Long.class, ligneId, transfertId);
    if (count == null || count == 0) {
      throw NotFoundException.entity("LigneTransfertStock", ligneId);
    }

    // Vérifier que le nouveau stock existe
    String checkStockSql = "SELECT COUNT(*) FROM stock_produits WHERE id = ?";
    Long stockCount = jdbcTemplate.queryForObject(checkStockSql, Long.class, request.getFkStock());
    if (stockCount == null || stockCount == 0) {
      throw NotFoundException.entity("Stock", request.getFkStock());
    }

    // Mettre à jour la ligne avec le nouveau stock
    String updateSql =
        "UPDATE lignes_transferts_stock SET fkStock = ?, dateupdate = ?, userupdateid = ?";
    java.util.List<Object> params = new java.util.ArrayList<>();
    params.add(request.getFkStock());
    params.add(java.sql.Timestamp.valueOf(LocalDateTime.now()));
    params.add(currentUserId);

    if (request.getQuantite() != null) {
      updateSql += ", quantite = ?";
      params.add(request.getQuantite());
    }

    updateSql += " WHERE id = ? AND fkTransfertStock = ?";
    params.add(ligneId);
    params.add(transfertId);

    int rows = jdbcTemplate.update(updateSql, params.toArray());
    if (rows == 0) {
      throw new BusinessException("Échec du remplacement du produit");
    }

    // Récupérer la ligne mise à jour
    String selectSql = "SELECT lts.id, lts.fkTransfertStock, lts.fkStock, "
        + "p.nomcommercial as stockNomCommercial, p.nomscientifique as stockNomScientifique, "
        + "f.designation as stockForme, d.designation as stockDosage, c.designation as stockConditionnement, "
        + "lts.quantiteDemandee, lts.quantite, " + "COALESCE(s.qte, 0) as quantiteEnStock, "
        + "lts.datecreate, lts.dateupdate, lts.usercreateid, lts.userupdateid "
        + "FROM lignes_transferts_stock lts " + "LEFT JOIN stock_produits s ON lts.fkStock = s.id "
        + "LEFT JOIN produits p ON s.fkProduits = p.id " + "LEFT JOIN formes f ON p.fkForme = f.id "
        + "LEFT JOIN dosages d ON p.fkDosage = d.id "
        + "LEFT JOIN conditionnements c ON p.fkConditionnement = c.id " + "WHERE lts.id = ?";

    return jdbcTemplate.query(selectSql, TransfertStockQueryService.LIGNE_TRANSFERT_MAPPER, ligneId)
        .stream().findFirst()
        .orElseThrow(() -> NotFoundException.entity("LigneTransfertStock", ligneId));
  }

  /**
   * Supprime une ligne de transfert.
   */
  public void deleteLigne(Long transfertId, Long ligneId) {
    log.debug("Suppression de la ligne - transfertId: {}, ligneId: {}", transfertId, ligneId);

    // Vérifier que la ligne existe et appartient au transfert
    String checkSql =
        "SELECT COUNT(*) FROM lignes_transferts_stock WHERE id = ? AND fkTransfertStock = ?";
    Long count = jdbcTemplate.queryForObject(checkSql, Long.class, ligneId, transfertId);
    if (count == null || count == 0) {
      throw NotFoundException.entity("LigneTransfertStock", ligneId);
    }

    String deleteSql = "DELETE FROM lignes_transferts_stock WHERE id = ? AND fkTransfertStock = ?";
    int rows = jdbcTemplate.update(deleteSql, ligneId, transfertId);
    if (rows == 0) {
      throw new BusinessException("Échec de la suppression de la ligne");
    }

    log.info("Ligne supprimée avec succès: ligneId: {}", ligneId);
  }

  /**
   * Met à jour le statut d'un transfert.
   * <p>Si le statut demandé est {@code ANNULEE}, délègue à {@link #annuler} pour restaurer le stock.
   */
  public TransfertStockResponse updateStatus(Long id, UpdateTransfertStatusRequest request,
      Long currentUserId) {
    log.debug("Mise à jour du statut du transfert ID: {} -> {}", id, request.getStatut());

    if ("ANNULEE".equals(request.getStatut())) {
      return annuler(id, currentUserId);
    }

    // Vérifier que le transfert existe
    String checkSql = "SELECT COUNT(*) FROM transferts_stock WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(checkSql, Long.class, id);
    if (count == null || count == 0) {
      throw NotFoundException.entity("TransfertStock", id);
    }

    // Construire la requête SQL de mise à jour
    StringBuilder updateSql = new StringBuilder(
        "UPDATE transferts_stock SET statut = ?, dateupdate = ?, userupdateid = ?");
    java.util.List<Object> params = new java.util.ArrayList<>();
    params.add(request.getStatut());
    params.add(java.sql.Timestamp.valueOf(LocalDateTime.now()));
    params.add(currentUserId);

    updateSql.append(" WHERE id = ?");
    params.add(id);

    int rows = jdbcTemplate.update(updateSql.toString(), params.toArray());
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour du statut du transfert");
    }

    log.info("Statut du transfert mis à jour avec succès: ID={}, statut={}", id,
        request.getStatut());

    // Récupérer le transfert mis à jour
    return transfertStockQueryService.findById(id);
  }

  /**
   * Annule un transfert en attente de réception (TRANSFEREE) : restaure le stock source,
   * remet la réquisition à VALIDEE et annule les brouillons de réception.
   */
  public TransfertStockResponse annuler(Long id, Long currentUserId) {
    log.debug("Annulation du transfert de stock ID: {}", id);

    java.util.Map<String, Object> row;
    try {
      row = jdbcTemplate.queryForMap(
          "SELECT id, statut, fkRequisition FROM transferts_stock WHERE id = ?", id);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw NotFoundException.entity("TransfertStock", id);
    }

    String statut = String.valueOf(row.get("statut"));
    Long fkRequisition = toLong(row.get("fkRequisition"));

    if ("ANNULEE".equals(statut)) {
      throw new BusinessException("Ce transfert est déjà annulé");
    }
    if ("RECEPTIONNEE".equals(statut)) {
      throw new BusinessException(
          "Impossible d'annuler un transfert déjà réceptionné");
    }
    if (!"TRANSFEREE".equals(statut) && !"EN ATTENTE".equals(statut)) {
      throw new BusinessException(
          "Seuls les transferts en attente ou transférés (non réceptionnés) peuvent être annulés");
    }

    Long receptionsValidees = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM reception_stock WHERE fkTransfert = ? AND statut = 'RECEPTIONNEE'",
        Long.class, id);
    if (receptionsValidees != null && receptionsValidees > 0) {
      throw new BusinessException(
          "Impossible d'annuler : une réception a déjà été validée pour ce transfert");
    }

    LocalDateTime now = LocalDateTime.now();
    java.sql.Timestamp ts = java.sql.Timestamp.valueOf(now);

    if ("TRANSFEREE".equals(statut)) {
      restoreStockFromLignes(id, currentUserId, ts);
    }

    jdbcTemplate.update(
        """
        UPDATE reception_stock
        SET statut = 'ANNULEE', dateupdate = ?, userupdateid = ?
        WHERE fkTransfert = ? AND statut = 'EN ATTENTE'
        """,
        ts, currentUserId, id);

    int rows = jdbcTemplate.update(
        """
        UPDATE transferts_stock
        SET statut = 'ANNULEE', dateupdate = ?, userupdateid = ?
        WHERE id = ? AND statut IN ('EN ATTENTE', 'TRANSFEREE')
        """,
        ts, currentUserId, id);
    if (rows == 0) {
      throw new BusinessException("Échec de l'annulation du transfert");
    }

    if (fkRequisition != null) {
      jdbcTemplate.update(
          """
          UPDATE requisitions
          SET statut = 'VALIDEE', dateupdate = ?, userupdateid = ?
          WHERE id = ? AND statut = 'TRANSFEREE'
          """,
          ts, currentUserId, fkRequisition);
    }

    log.info("Transfert de stock annulé: ID={}, statut précédent={}, stock restauré={}",
        id, statut, "TRANSFEREE".equals(statut));
    return transfertStockQueryService.findById(id);
  }

  private void restoreStockFromLignes(Long transfertId, Long currentUserId, java.sql.Timestamp ts) {
    java.util.List<java.util.Map<String, Object>> lignes = jdbcTemplate.queryForList(
        """
        SELECT fkStock, COALESCE(quantite, 0) AS quantite
        FROM lignes_transferts_stock
        WHERE fkTransfertStock = ? AND fkStock IS NOT NULL AND COALESCE(quantite, 0) > 0
        """,
        transfertId);

    for (java.util.Map<String, Object> ligne : lignes) {
      Long fkStock = toLong(ligne.get("fkStock"));
      Double quantite = toDouble(ligne.get("quantite"));
      if (fkStock == null || quantite == null || quantite <= 0) {
        continue;
      }
      int updated = jdbcTemplate.update(
          """
          UPDATE stock_produits
          SET qte = qte + ?, dateupdate = ?, userupdateid = ?
          WHERE id = ?
          """,
          quantite, ts, currentUserId, fkStock);
      if (updated == 0) {
        throw new BusinessException(
            "Impossible de restaurer le stock (lot introuvable, ID: " + fkStock + ")");
      }
      log.debug("Stock restauré - stock ID: {}, quantité: {}", fkStock, quantite);
    }
  }

  private static Long toLong(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Double toDouble(Object v) {
    if (v == null) {
      return 0d;
    }
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(v));
    } catch (NumberFormatException e) {
      return 0d;
    }
  }
}

