package cd.shad.erp.cmk.cmkerp.stocks.commandes.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.*;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.repository.CommandesRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class CommandesJdbcRepositoryImpl extends AbstractJdbcRepository implements CommandesRepository {

  public CommandesJdbcRepositoryImpl(
      @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
    super(jdbcTemplate, namedJdbcTemplate);
  }

  private static Timestamp ts(LocalDateTime t) {
    return t != null ? Timestamp.valueOf(t) : null;
  }

  private static LocalDateTime fromTs(Timestamp t) {
    return t != null ? t.toLocalDateTime() : null;
  }

  private Long extractKey(KeyHolder kh) {
    if (kh.getKey() != null) {
      return kh.getKey().longValue();
    }
    Long last = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    return last != null && last > 0 ? last : null;
  }

  private final RowMapper<DemandeCotation> DEMANDE_MAPPER = (rs, i) -> DemandeCotation.builder()
      .id(rs.getLong("id")).numero(rs.getString("numero")).objet(rs.getString("objet"))
      .description(rs.getString("description")).fkPharmacieDemandeur(rs.getLong("fk_pharmacie_demandeur"))
      .dateLimiteReponse(fromTs(rs.getTimestamp("date_limite_reponse")))
      .dateLivraisonSouhaitee(rs.getDate("date_livraison_souhaitee") != null ? rs.getDate("date_livraison_souhaitee").toLocalDate() : null)
      .lieuLivraison(rs.getString("lieu_livraison")).conditions(rs.getString("conditions"))
      .statut(rs.getString("statut")).dateCreate(fromTs(rs.getTimestamp("datecreate")))
      .dateUpdate(fromTs(rs.getTimestamp("dateupdate")))
      .userCreatedId(rs.getObject("usercreateid", Long.class)).userUpdatedId(rs.getObject("userupdateid", Long.class))
      .build();

  private final RowMapper<LigneDemandeCotation> LIGNE_DC_MAPPER = (rs, i) -> LigneDemandeCotation.builder()
      .id(rs.getLong("id")).fkDemandeCotation(rs.getLong("fk_demande_cotation"))
      .fkProduit(rs.getLong("fk_produit")).fkCategorie(rs.getObject("fk_categorie", Long.class))
      .quantite(rs.getBigDecimal("quantite")).specifications(rs.getString("specifications"))
      .ordre(rs.getInt("ordre")).dateCreate(fromTs(rs.getTimestamp("datecreate")))
      .userCreatedId(rs.getObject("usercreateid", Long.class)).build();

  private final RowMapper<InvitationFournisseur> INV_MAPPER = (rs, i) -> InvitationFournisseur.builder()
      .id(rs.getLong("id")).fkDemandeCotation(rs.getLong("fk_demande_cotation"))
      .fkFournisseur(rs.getLong("fk_fournisseur")).publicToken(rs.getString("public_token"))
      .accessCodeHash(rs.getString("access_code_hash")).sessionTokenHash(rs.getString("session_token_hash"))
      .sessionExpiresAt(fromTs(rs.getTimestamp("session_expires_at")))
      .unlockAttempts(rs.getInt("unlock_attempts"))
      .unlockLockedUntil(fromTs(rs.getTimestamp("unlock_locked_until")))
      .statut(rs.getString("statut")).expiresAt(fromTs(rs.getTimestamp("expires_at")))
      .openedAt(fromTs(rs.getTimestamp("opened_at"))).submittedAt(fromTs(rs.getTimestamp("submitted_at")))
      .relances(rs.getInt("relances")).dateCreate(fromTs(rs.getTimestamp("datecreate")))
      .dateUpdate(fromTs(rs.getTimestamp("dateupdate")))
      .userCreatedId(rs.getObject("usercreateid", Long.class)).userUpdatedId(rs.getObject("userupdateid", Long.class))
      .build();

  private final RowMapper<OffreFournisseur> OFFRE_MAPPER = (rs, i) -> OffreFournisseur.builder()
      .id(rs.getLong("id")).fkInvitation(rs.getLong("fk_invitation"))
      .fkDemandeCotation(rs.getLong("fk_demande_cotation")).fkFournisseur(rs.getLong("fk_fournisseur"))
      .devise(rs.getString("devise")).tauxDeclare(rs.getBigDecimal("taux_declare"))
      .validiteJusquau(rs.getDate("validite_jusquau") != null ? rs.getDate("validite_jusquau").toLocalDate() : null)
      .fraisLivraison(rs.getBigDecimal("frais_livraison")).conditions(rs.getString("conditions"))
      .statut(rs.getString("statut")).versionNo(rs.getInt("version_no"))
      .dateSoumission(fromTs(rs.getTimestamp("date_soumission"))).lockedAt(fromTs(rs.getTimestamp("locked_at")))
      .idempotenceSubmitKey(rs.getString("idempotence_submit_key"))
      .dateCreate(fromTs(rs.getTimestamp("datecreate"))).dateUpdate(fromTs(rs.getTimestamp("dateupdate")))
      .build();

  private final RowMapper<LigneOffreFournisseur> LIGNE_OFFRE_MAPPER = (rs, i) -> LigneOffreFournisseur.builder()
      .id(rs.getLong("id")).fkOffre(rs.getLong("fk_offre")).fkLigneDemande(rs.getLong("fk_ligne_demande"))
      .prixOriginal(rs.getBigDecimal("prix_original")).devise(rs.getString("devise"))
      .taux(rs.getBigDecimal("taux")).fkEchangeDevise(rs.getObject("fk_echange_devise", Long.class))
      .prixUsd(rs.getBigDecimal("prix_usd")).prixCdf(rs.getBigDecimal("prix_cdf"))
      .quantiteDisponible(rs.getBigDecimal("quantite_disponible")).delaiJours(rs.getObject("delai_jours", Integer.class))
      .substitution(rs.getString("substitution")).commentaire(rs.getString("commentaire"))
      .dateCreate(fromTs(rs.getTimestamp("datecreate"))).build();

  private final RowMapper<BonCommande> BON_MAPPER = (rs, i) -> BonCommande.builder()
      .id(rs.getLong("id")).numero(rs.getString("numero"))
      .fkDemandeCotation(rs.getObject("fk_demande_cotation", Long.class))
      .fkAttribution(rs.getObject("fk_attribution", Long.class))
      .fkFournisseur(rs.getLong("fk_fournisseur")).fkPharmacie(rs.getObject("fk_pharmacie", Long.class))
      .fkEchangeDevise(rs.getObject("fk_echange_devise", Long.class)).statut(rs.getString("statut"))
      .montantTotalUsd(rs.getBigDecimal("montant_total_usd"))
      .dateCommande(rs.getDate("date_commande") != null ? rs.getDate("date_commande").toLocalDate() : null)
      .dateLivraisonPrevue(rs.getDate("date_livraison_prevue") != null ? rs.getDate("date_livraison_prevue").toLocalDate() : null)
      .dateCreate(fromTs(rs.getTimestamp("datecreate"))).dateUpdate(fromTs(rs.getTimestamp("dateupdate")))
      .userCreatedId(rs.getObject("usercreateid", Long.class)).userUpdatedId(rs.getObject("userupdateid", Long.class))
      .build();

  private final RowMapper<LigneBonCommande> LIGNE_BON_MAPPER = (rs, i) -> LigneBonCommande.builder()
      .id(rs.getLong("id")).fkBonCommande(rs.getLong("fk_bon_commande"))
      .fkLigneDemande(rs.getObject("fk_ligne_demande", Long.class)).fkProduit(rs.getLong("fk_produit"))
      .quantiteCommandee(rs.getBigDecimal("quantite_commandee")).quantiteRecue(rs.getBigDecimal("quantite_recue"))
      .prixUnitaireUsd(rs.getBigDecimal("prix_unitaire_usd")).montantLigneUsd(rs.getBigDecimal("montant_ligne_usd"))
      .prixOriginal(rs.getBigDecimal("prix_original")).devise(rs.getString("devise")).taux(rs.getBigDecimal("taux"))
      .dateCreate(fromTs(rs.getTimestamp("datecreate"))).dateUpdate(fromTs(rs.getTimestamp("dateupdate")))
      .build();

  private final RowMapper<ReceptionCommande> RECEP_MAPPER = (rs, i) -> ReceptionCommande.builder()
      .id(rs.getLong("id")).fkBonCommande(rs.getLong("fk_bon_commande")).numero(rs.getString("numero"))
      .statut(rs.getString("statut"))
      .dateReception(rs.getDate("date_reception") != null ? rs.getDate("date_reception").toLocalDate() : null)
      .fkApprovisionnement(rs.getObject("fk_approvisionnement", Long.class)).commentaire(rs.getString("commentaire"))
      .dateCreate(fromTs(rs.getTimestamp("datecreate"))).dateUpdate(fromTs(rs.getTimestamp("dateupdate")))
      .userCreatedId(rs.getObject("usercreateid", Long.class)).userUpdatedId(rs.getObject("userupdateid", Long.class))
      .build();

  @Override
  public Optional<DemandeCotation> findDemandeById(Long id) {
    return queryForOptional("SELECT * FROM demandes_cotation WHERE id = ?", DEMANDE_MAPPER, id);
  }

  @Override
  public List<DemandeCotation> findDemandes(int offset, int limit, String statut, Long fkPharmacie, String search) {
    StringBuilder sql = new StringBuilder("SELECT * FROM demandes_cotation WHERE 1=1");
    List<Object> params = new ArrayList<>();
    appendDemandeFilters(sql, params, statut, fkPharmacie, search);
    sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), DEMANDE_MAPPER, params.toArray());
  }

  @Override
  public long countDemandes(String statut, Long fkPharmacie, String search) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM demandes_cotation WHERE 1=1");
    List<Object> params = new ArrayList<>();
    appendDemandeFilters(sql, params, statut, fkPharmacie, search);
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0L;
  }

  private void appendDemandeFilters(StringBuilder sql, List<Object> params, String statut, Long fkPharmacie, String search) {
    if (statut != null && !statut.isBlank()) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    if (fkPharmacie != null) {
      sql.append(" AND fk_pharmacie_demandeur = ?");
      params.add(fkPharmacie);
    }
    if (search != null && !search.isBlank()) {
      sql.append(" AND (LOWER(numero) LIKE LOWER(?) OR LOWER(objet) LIKE LOWER(?))");
      String p = "%" + search.trim() + "%";
      params.add(p);
      params.add(p);
    }
  }

  @Override
  public int saveDemande(DemandeCotation d) {
    String sql = "INSERT INTO demandes_cotation (numero, objet, description, fk_pharmacie_demandeur, date_limite_reponse, "
        + "date_livraison_souhaitee, lieu_livraison, conditions, statut, datecreate, usercreateid) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, d.getNumero());
      ps.setString(2, d.getObjet());
      ps.setString(3, d.getDescription());
      ps.setLong(4, d.getFkPharmacieDemandeur());
      ps.setTimestamp(5, ts(d.getDateLimiteReponse()));
      if (d.getDateLivraisonSouhaitee() != null) {
        ps.setDate(6, java.sql.Date.valueOf(d.getDateLivraisonSouhaitee()));
      } else {
        ps.setNull(6, java.sql.Types.DATE);
      }
      ps.setString(7, d.getLieuLivraison());
      ps.setString(8, d.getConditions());
      ps.setString(9, d.getStatut() != null ? d.getStatut() : "BROUILLON");
      ps.setTimestamp(10, ts(d.getDateCreate() != null ? d.getDateCreate() : LocalDateTime.now()));
      if (d.getUserCreatedId() != null) {
        ps.setLong(11, d.getUserCreatedId());
      } else {
        ps.setNull(11, java.sql.Types.BIGINT);
      }
      return ps;
    }, kh);
    if (rows > 0) {
      d.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int updateDemande(DemandeCotation d) {
    return update(
        "UPDATE demandes_cotation SET objet=?, description=?, date_limite_reponse=?, date_livraison_souhaitee=?, "
            + "lieu_livraison=?, conditions=?, statut=?, dateupdate=?, userupdateid=? WHERE id=?",
        d.getObjet(), d.getDescription(), ts(d.getDateLimiteReponse()),
        d.getDateLivraisonSouhaitee() != null ? java.sql.Date.valueOf(d.getDateLivraisonSouhaitee()) : null,
        d.getLieuLivraison(), d.getConditions(), d.getStatut(),
        ts(d.getDateUpdate() != null ? d.getDateUpdate() : LocalDateTime.now()),
        d.getUserUpdatedId(), d.getId());
  }

  @Override
  public List<LigneDemandeCotation> findLignesDemande(Long demandeId) {
    return queryForList("SELECT * FROM lignes_demande_cotation WHERE fk_demande_cotation = ? ORDER BY ordre, id",
        LIGNE_DC_MAPPER, demandeId);
  }

  @Override
  public void deleteLignesDemande(Long demandeId) {
    update("DELETE FROM lignes_demande_cotation WHERE fk_demande_cotation = ?", demandeId);
  }

  @Override
  public int saveLigneDemande(LigneDemandeCotation l) {
    String sql = "INSERT INTO lignes_demande_cotation (fk_demande_cotation, fk_produit, fk_categorie, quantite, specifications, ordre, datecreate, usercreateid) "
        + "VALUES (?,?,?,?,?,?,?,?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, l.getFkDemandeCotation());
      ps.setLong(2, l.getFkProduit());
      if (l.getFkCategorie() != null) {
        ps.setLong(3, l.getFkCategorie());
      } else {
        ps.setNull(3, java.sql.Types.BIGINT);
      }
      ps.setBigDecimal(4, l.getQuantite());
      ps.setString(5, l.getSpecifications());
      ps.setInt(6, l.getOrdre() != null ? l.getOrdre() : 0);
      ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
      if (l.getUserCreatedId() != null) {
        ps.setLong(8, l.getUserCreatedId());
      } else {
        ps.setNull(8, java.sql.Types.BIGINT);
      }
      return ps;
    }, kh);
    if (rows > 0) {
      l.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public List<InvitationFournisseur> findInvitationsByDemande(Long demandeId) {
    return queryForList("SELECT * FROM invitations_fournisseur WHERE fk_demande_cotation = ?", INV_MAPPER, demandeId);
  }

  @Override
  public Optional<InvitationFournisseur> findInvitationById(Long id) {
    return queryForOptional("SELECT * FROM invitations_fournisseur WHERE id = ?", INV_MAPPER, id);
  }

  @Override
  public Optional<InvitationFournisseur> findInvitationByPublicToken(String token) {
    return queryForOptional("SELECT * FROM invitations_fournisseur WHERE public_token = ?", INV_MAPPER, token);
  }

  @Override
  public int saveInvitation(InvitationFournisseur inv) {
    String sql = "INSERT INTO invitations_fournisseur (fk_demande_cotation, fk_fournisseur, public_token, access_code_hash, statut, expires_at, datecreate, usercreateid) "
        + "VALUES (?,?,?,?,?,?,?,?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, inv.getFkDemandeCotation());
      ps.setLong(2, inv.getFkFournisseur());
      ps.setString(3, inv.getPublicToken());
      ps.setString(4, inv.getAccessCodeHash());
      ps.setString(5, inv.getStatut() != null ? inv.getStatut() : "CREEE");
      ps.setTimestamp(6, ts(inv.getExpiresAt()));
      ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
      if (inv.getUserCreatedId() != null) {
        ps.setLong(8, inv.getUserCreatedId());
      } else {
        ps.setNull(8, java.sql.Types.BIGINT);
      }
      return ps;
    }, kh);
    if (rows > 0) {
      inv.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int updateInvitation(InvitationFournisseur inv) {
    // access_code_hash: required so regenerer/envoyer/relancer persist the hash matching the plaintext returned once
    return update(
        "UPDATE invitations_fournisseur SET access_code_hash=?, statut=?, session_token_hash=?, session_expires_at=?, "
            + "unlock_attempts=?, unlock_locked_until=?, expires_at=?, opened_at=?, submitted_at=?, relances=?, "
            + "dateupdate=?, userupdateid=? WHERE id=?",
        inv.getAccessCodeHash(), inv.getStatut(), inv.getSessionTokenHash(), ts(inv.getSessionExpiresAt()),
        inv.getUnlockAttempts() != null ? inv.getUnlockAttempts() : 0, ts(inv.getUnlockLockedUntil()),
        ts(inv.getExpiresAt()), ts(inv.getOpenedAt()), ts(inv.getSubmittedAt()),
        inv.getRelances() != null ? inv.getRelances() : 0,
        Timestamp.valueOf(LocalDateTime.now()), inv.getUserUpdatedId(), inv.getId());
  }

  @Override
  public Optional<OffreFournisseur> findOffreById(Long id) {
    return queryForOptional("SELECT * FROM offres_fournisseur WHERE id = ?", OFFRE_MAPPER, id);
  }

  @Override
  public Optional<OffreFournisseur> findOffreByInvitation(Long invitationId) {
    return queryForOptional("SELECT * FROM offres_fournisseur WHERE fk_invitation = ?", OFFRE_MAPPER, invitationId);
  }

  @Override
  public List<OffreFournisseur> findOffres(int offset, int limit, Long fkDemande, String statut) {
    StringBuilder sql = new StringBuilder("SELECT * FROM offres_fournisseur WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (fkDemande != null) {
      sql.append(" AND fk_demande_cotation = ?");
      params.add(fkDemande);
    }
    if (statut != null && !statut.isBlank()) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), OFFRE_MAPPER, params.toArray());
  }

  @Override
  public long countOffres(Long fkDemande, String statut) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM offres_fournisseur WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (fkDemande != null) {
      sql.append(" AND fk_demande_cotation = ?");
      params.add(fkDemande);
    }
    if (statut != null && !statut.isBlank()) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0L;
  }

  @Override
  public int saveOffre(OffreFournisseur o) {
    String sql = "INSERT INTO offres_fournisseur (fk_invitation, fk_demande_cotation, fk_fournisseur, devise, taux_declare, "
        + "validite_jusquau, frais_livraison, conditions, statut, version_no, datecreate) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, o.getFkInvitation());
      ps.setLong(2, o.getFkDemandeCotation());
      ps.setLong(3, o.getFkFournisseur());
      ps.setString(4, o.getDevise());
      ps.setBigDecimal(5, o.getTauxDeclare());
      if (o.getValiditeJusquau() != null) {
        ps.setDate(6, java.sql.Date.valueOf(o.getValiditeJusquau()));
      } else {
        ps.setNull(6, java.sql.Types.DATE);
      }
      ps.setBigDecimal(7, o.getFraisLivraison());
      ps.setString(8, o.getConditions());
      ps.setString(9, o.getStatut() != null ? o.getStatut() : "BROUILLON");
      ps.setInt(10, o.getVersionNo() != null ? o.getVersionNo() : 1);
      ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
      return ps;
    }, kh);
    if (rows > 0) {
      o.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int updateOffre(OffreFournisseur o) {
    return update(
        "UPDATE offres_fournisseur SET devise=?, taux_declare=?, validite_jusquau=?, frais_livraison=?, conditions=?, "
            + "statut=?, version_no=?, date_soumission=?, locked_at=?, idempotence_submit_key=?, dateupdate=? WHERE id=?",
        o.getDevise(), o.getTauxDeclare(),
        o.getValiditeJusquau() != null ? java.sql.Date.valueOf(o.getValiditeJusquau()) : null,
        o.getFraisLivraison(), o.getConditions(), o.getStatut(), o.getVersionNo(),
        ts(o.getDateSoumission()), ts(o.getLockedAt()), o.getIdempotenceSubmitKey(),
        Timestamp.valueOf(LocalDateTime.now()), o.getId());
  }

  @Override
  public List<LigneOffreFournisseur> findLignesOffre(Long offreId) {
    return queryForList("SELECT * FROM lignes_offre_fournisseur WHERE fk_offre = ?", LIGNE_OFFRE_MAPPER, offreId);
  }

  @Override
  public void deleteLignesOffre(Long offreId) {
    update("DELETE FROM lignes_offre_fournisseur WHERE fk_offre = ?", offreId);
  }

  @Override
  public int saveLigneOffre(LigneOffreFournisseur l) {
    String sql = "INSERT INTO lignes_offre_fournisseur (fk_offre, fk_ligne_demande, prix_original, devise, taux, fk_echange_devise, "
        + "prix_usd, prix_cdf, quantite_disponible, delai_jours, substitution, commentaire, datecreate) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      int idx = 1;
      ps.setLong(idx++, l.getFkOffre());
      ps.setLong(idx++, l.getFkLigneDemande());
      ps.setBigDecimal(idx++, l.getPrixOriginal());
      ps.setString(idx++, l.getDevise());
      ps.setBigDecimal(idx++, l.getTaux());
      if (l.getFkEchangeDevise() != null) {
        ps.setLong(idx++, l.getFkEchangeDevise());
      } else {
        ps.setNull(idx++, java.sql.Types.BIGINT);
      }
      ps.setBigDecimal(idx++, l.getPrixUsd());
      ps.setBigDecimal(idx++, l.getPrixCdf());
      ps.setBigDecimal(idx++, l.getQuantiteDisponible());
      if (l.getDelaiJours() != null) {
        ps.setInt(idx++, l.getDelaiJours());
      } else {
        ps.setNull(idx++, java.sql.Types.INTEGER);
      }
      ps.setString(idx++, l.getSubstitution());
      ps.setString(idx++, l.getCommentaire());
      ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));
      return ps;
    }, kh);
    if (rows > 0) {
      l.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int saveVersionOffre(Long offreId, int versionNo, String snapshotJson, Long userId) {
    return update(
        "INSERT INTO versions_offre_fournisseur (fk_offre, version_no, snapshot_json, datecreate, usercreateid) VALUES (?,?,?,NOW(),?)",
        offreId, versionNo, snapshotJson, userId);
  }

  @Override
  public int savePieceJointe(Long offreId, String nom, String mime, Long taille, String storageKey) {
    return update(
        "INSERT INTO pieces_jointes_offre (fk_offre, nom_fichier, mime_type, taille, storage_key, datecreate) VALUES (?,?,?,?,?,NOW())",
        offreId, nom, mime, taille, storageKey);
  }

  @Override
  public List<Map<String, Object>> findPiecesJointes(Long offreId) {
    return jdbcTemplate.queryForList(
        "SELECT id, nom_fichier AS nomFichier, mime_type AS mimeType, taille, storage_key AS storageKey, datecreate AS dateCreate "
            + "FROM pieces_jointes_offre WHERE fk_offre = ?",
        offreId);
  }

  @Override
  public int saveAttribution(AttributionCotation a) {
    String sql = "INSERT INTO attributions_cotation (fk_demande_cotation, scope, justification, fk_categorie, datecreate, usercreateid) VALUES (?,?,?,?,NOW(),?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, a.getFkDemandeCotation());
      ps.setString(2, a.getScope());
      ps.setString(3, a.getJustification());
      if (a.getFkCategorie() != null) {
        ps.setLong(4, a.getFkCategorie());
      } else {
        ps.setNull(4, java.sql.Types.BIGINT);
      }
      if (a.getUserCreatedId() != null) {
        ps.setLong(5, a.getUserCreatedId());
      } else {
        ps.setNull(5, java.sql.Types.BIGINT);
      }
      return ps;
    }, kh);
    if (rows > 0) {
      a.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int saveLigneAttribution(LigneAttribution l) {
    return update(
        "INSERT INTO lignes_attribution (fk_attribution, fk_ligne_demande, fk_fournisseur, quantite_attribuee, motif, datecreate) VALUES (?,?,?,?,?,NOW())",
        l.getFkAttribution(), l.getFkLigneDemande(), l.getFkFournisseur(), l.getQuantiteAttribuee(), l.getMotif());
  }

  @Override
  public Optional<BonCommande> findBonById(Long id) {
    return queryForOptional("SELECT * FROM bons_commande WHERE id = ?", BON_MAPPER, id);
  }

  @Override
  public List<BonCommande> findBons(int offset, int limit, String statut, Long fkFournisseur, String search) {
    StringBuilder sql = new StringBuilder("SELECT * FROM bons_commande WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (statut != null && !statut.isBlank()) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    if (fkFournisseur != null) {
      sql.append(" AND fk_fournisseur = ?");
      params.add(fkFournisseur);
    }
    if (search != null && !search.isBlank()) {
      sql.append(" AND LOWER(numero) LIKE LOWER(?)");
      params.add("%" + search.trim() + "%");
    }
    sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), BON_MAPPER, params.toArray());
  }

  @Override
  public long countBons(String statut, Long fkFournisseur, String search) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM bons_commande WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (statut != null && !statut.isBlank()) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    if (fkFournisseur != null) {
      sql.append(" AND fk_fournisseur = ?");
      params.add(fkFournisseur);
    }
    if (search != null && !search.isBlank()) {
      sql.append(" AND LOWER(numero) LIKE LOWER(?)");
      params.add("%" + search.trim() + "%");
    }
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0L;
  }

  @Override
  public int saveBon(BonCommande b) {
    String sql = "INSERT INTO bons_commande (numero, fk_demande_cotation, fk_attribution, fk_fournisseur, fk_pharmacie, "
        + "fk_echange_devise, statut, montant_total_usd, date_commande, date_livraison_prevue, datecreate, usercreateid) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, b.getNumero());
      setLongOrNull(ps, 2, b.getFkDemandeCotation());
      setLongOrNull(ps, 3, b.getFkAttribution());
      ps.setLong(4, b.getFkFournisseur());
      setLongOrNull(ps, 5, b.getFkPharmacie());
      setLongOrNull(ps, 6, b.getFkEchangeDevise());
      ps.setString(7, b.getStatut() != null ? b.getStatut() : "BROUILLON");
      ps.setBigDecimal(8, b.getMontantTotalUsd());
      if (b.getDateCommande() != null) {
        ps.setDate(9, java.sql.Date.valueOf(b.getDateCommande()));
      } else {
        ps.setNull(9, java.sql.Types.DATE);
      }
      if (b.getDateLivraisonPrevue() != null) {
        ps.setDate(10, java.sql.Date.valueOf(b.getDateLivraisonPrevue()));
      } else {
        ps.setNull(10, java.sql.Types.DATE);
      }
      setLongOrNull(ps, 11, b.getUserCreatedId());
      return ps;
    }, kh);
    if (rows > 0) {
      b.setId(extractKey(kh));
    }
    return rows;
  }

  private static void setLongOrNull(PreparedStatement ps, int idx, Long v) throws java.sql.SQLException {
    if (v != null) {
      ps.setLong(idx, v);
    } else {
      ps.setNull(idx, java.sql.Types.BIGINT);
    }
  }

  @Override
  public int updateBon(BonCommande b) {
    return update(
        "UPDATE bons_commande SET statut=?, montant_total_usd=?, date_commande=?, date_livraison_prevue=?, dateupdate=?, userupdateid=? WHERE id=?",
        b.getStatut(), b.getMontantTotalUsd(),
        b.getDateCommande() != null ? java.sql.Date.valueOf(b.getDateCommande()) : null,
        b.getDateLivraisonPrevue() != null ? java.sql.Date.valueOf(b.getDateLivraisonPrevue()) : null,
        Timestamp.valueOf(LocalDateTime.now()), b.getUserUpdatedId(), b.getId());
  }

  @Override
  public List<LigneBonCommande> findLignesBon(Long bonId) {
    return queryForList("SELECT * FROM lignes_bon_commande WHERE fk_bon_commande = ?", LIGNE_BON_MAPPER, bonId);
  }

  @Override
  public int saveLigneBon(LigneBonCommande l) {
    String sql = "INSERT INTO lignes_bon_commande (fk_bon_commande, fk_ligne_demande, fk_produit, quantite_commandee, quantite_recue, "
        + "prix_unitaire_usd, montant_ligne_usd, prix_original, devise, taux, datecreate) VALUES (?,?,?,?,?,?,?,?,?,?,NOW())";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, l.getFkBonCommande());
      setLongOrNull(ps, 2, l.getFkLigneDemande());
      ps.setLong(3, l.getFkProduit());
      ps.setBigDecimal(4, l.getQuantiteCommandee());
      ps.setBigDecimal(5, l.getQuantiteRecue() != null ? l.getQuantiteRecue() : java.math.BigDecimal.ZERO);
      ps.setBigDecimal(6, l.getPrixUnitaireUsd());
      ps.setBigDecimal(7, l.getMontantLigneUsd());
      ps.setBigDecimal(8, l.getPrixOriginal());
      ps.setString(9, l.getDevise());
      ps.setBigDecimal(10, l.getTaux());
      return ps;
    }, kh);
    if (rows > 0) {
      l.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int updateLigneBon(LigneBonCommande l) {
    return update(
        "UPDATE lignes_bon_commande SET quantite_recue=?, dateupdate=NOW() WHERE id=?",
        l.getQuantiteRecue(), l.getId());
  }

  @Override
  public Optional<ReceptionCommande> findReceptionById(Long id) {
    return queryForOptional("SELECT * FROM receptions_commande WHERE id = ?", RECEP_MAPPER, id);
  }

  @Override
  public List<ReceptionCommande> findReceptionsByBon(Long bonId) {
    return queryForList("SELECT * FROM receptions_commande WHERE fk_bon_commande = ? ORDER BY id DESC", RECEP_MAPPER, bonId);
  }

  @Override
  public int saveReception(ReceptionCommande r) {
    String sql = "INSERT INTO receptions_commande (fk_bon_commande, numero, statut, date_reception, fk_approvisionnement, commentaire, datecreate, usercreateid) "
        + "VALUES (?,?,?,?,?,?,NOW(),?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, r.getFkBonCommande());
      ps.setString(2, r.getNumero());
      ps.setString(3, r.getStatut() != null ? r.getStatut() : "BROUILLON");
      if (r.getDateReception() != null) {
        ps.setDate(4, java.sql.Date.valueOf(r.getDateReception()));
      } else {
        ps.setNull(4, java.sql.Types.DATE);
      }
      setLongOrNull(ps, 5, r.getFkApprovisionnement());
      ps.setString(6, r.getCommentaire());
      setLongOrNull(ps, 7, r.getUserCreatedId());
      return ps;
    }, kh);
    if (rows > 0) {
      r.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int updateReception(ReceptionCommande r) {
    return update(
        "UPDATE receptions_commande SET statut=?, date_reception=?, fk_approvisionnement=?, commentaire=?, dateupdate=NOW(), userupdateid=? WHERE id=?",
        r.getStatut(),
        r.getDateReception() != null ? java.sql.Date.valueOf(r.getDateReception()) : null,
        r.getFkApprovisionnement(), r.getCommentaire(), r.getUserUpdatedId(), r.getId());
  }

  @Override
  public int saveLigneReception(LigneReceptionCommande l) {
    return update(
        "INSERT INTO lignes_reception_commande (fk_reception, fk_ligne_bon_commande, quantite_recue, lot, date_peremption, datecreate) VALUES (?,?,?,?,?,NOW())",
        l.getFkReception(), l.getFkLigneBonCommande(), l.getQuantiteRecue(), l.getLot(),
        l.getDatePeremption() != null ? java.sql.Date.valueOf(l.getDatePeremption()) : null);
  }

  @Override
  public List<LigneReceptionCommande> findLignesReception(Long receptionId) {
    return jdbcTemplate.query(
        "SELECT * FROM lignes_reception_commande WHERE fk_reception = ?",
        (rs, i) -> LigneReceptionCommande.builder()
            .id(rs.getLong("id")).fkReception(rs.getLong("fk_reception"))
            .fkLigneBonCommande(rs.getLong("fk_ligne_bon_commande"))
            .quantiteRecue(rs.getBigDecimal("quantite_recue")).lot(rs.getString("lot"))
            .datePeremption(rs.getDate("date_peremption") != null ? rs.getDate("date_peremption").toLocalDate() : null)
            .dateCreate(fromTs(rs.getTimestamp("datecreate"))).build(),
        receptionId);
  }

  @Override
  public Optional<ParamScoreFournisseur> findParamScore() {
    return queryForOptional(
        "SELECT * FROM param_score_fournisseur ORDER BY id LIMIT 1",
        (rs, i) -> ParamScoreFournisseur.builder()
            .id(rs.getLong("id"))
            .poidsDelais(rs.getBigDecimal("poids_delais"))
            .poidsQualite(rs.getBigDecimal("poids_qualite"))
            .poidsPrix(rs.getBigDecimal("poids_prix"))
            .poidsCompletude(rs.getBigDecimal("poids_completude"))
            .poidsReactivite(rs.getBigDecimal("poids_reactivite"))
            .dateCreate(fromTs(rs.getTimestamp("datecreate")))
            .dateUpdate(fromTs(rs.getTimestamp("dateupdate")))
            .userUpdatedId(rs.getObject("userupdateid", Long.class)).build());
  }

  @Override
  public int updateParamScore(ParamScoreFournisseur p) {
    return update(
        "UPDATE param_score_fournisseur SET poids_delais=?, poids_qualite=?, poids_prix=?, poids_completude=?, poids_reactivite=?, dateupdate=NOW(), userupdateid=? WHERE id=?",
        p.getPoidsDelais(), p.getPoidsQualite(), p.getPoidsPrix(), p.getPoidsCompletude(), p.getPoidsReactivite(),
        p.getUserUpdatedId(), p.getId());
  }

  @Override
  public int saveEvaluation(EvaluationFournisseur e) {
    String sql = "INSERT INTO evaluations_fournisseur (fk_fournisseur, fk_bon_commande, fk_reception, note_delais, note_qualite, "
        + "note_prix, note_completude, note_reactivite, score_global, commentaire, datecreate, usercreateid) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),?)";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, e.getFkFournisseur());
      setLongOrNull(ps, 2, e.getFkBonCommande());
      setLongOrNull(ps, 3, e.getFkReception());
      ps.setBigDecimal(4, e.getNoteDelais());
      ps.setBigDecimal(5, e.getNoteQualite());
      ps.setBigDecimal(6, e.getNotePrix());
      ps.setBigDecimal(7, e.getNoteCompletude());
      ps.setBigDecimal(8, e.getNoteReactivite());
      ps.setBigDecimal(9, e.getScoreGlobal());
      ps.setString(10, e.getCommentaire());
      setLongOrNull(ps, 11, e.getUserCreatedId());
      return ps;
    }, kh);
    if (rows > 0) {
      e.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public List<EvaluationFournisseur> findEvaluations(int offset, int limit, Long fkFournisseur) {
    StringBuilder sql = new StringBuilder("SELECT * FROM evaluations_fournisseur WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (fkFournisseur != null) {
      sql.append(" AND fk_fournisseur = ?");
      params.add(fkFournisseur);
    }
    sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), (rs, i) -> EvaluationFournisseur.builder()
        .id(rs.getLong("id")).fkFournisseur(rs.getLong("fk_fournisseur"))
        .fkBonCommande(rs.getObject("fk_bon_commande", Long.class))
        .fkReception(rs.getObject("fk_reception", Long.class))
        .noteDelais(rs.getBigDecimal("note_delais")).noteQualite(rs.getBigDecimal("note_qualite"))
        .notePrix(rs.getBigDecimal("note_prix")).noteCompletude(rs.getBigDecimal("note_completude"))
        .noteReactivite(rs.getBigDecimal("note_reactivite")).scoreGlobal(rs.getBigDecimal("score_global"))
        .commentaire(rs.getString("commentaire")).dateCreate(fromTs(rs.getTimestamp("datecreate")))
        .userCreatedId(rs.getObject("usercreateid", Long.class)).build(), params.toArray());
  }

  @Override
  public long countEvaluations(Long fkFournisseur) {
    if (fkFournisseur != null) {
      Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM evaluations_fournisseur WHERE fk_fournisseur = ?", Long.class, fkFournisseur);
      return c != null ? c : 0L;
    }
    Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM evaluations_fournisseur", Long.class);
    return c != null ? c : 0L;
  }

  @Override
  public Optional<DemandeModifFournisseur> findModifById(Long id) {
    return queryForOptional("SELECT * FROM demandes_modif_fournisseur WHERE id = ?",
        (rs, i) -> DemandeModifFournisseur.builder()
            .id(rs.getLong("id")).fkFournisseur(rs.getLong("fk_fournisseur")).statut(rs.getString("statut"))
            .motif(rs.getString("motif")).commentaireDecision(rs.getString("commentaire_decision"))
            .dateDecision(fromTs(rs.getTimestamp("date_decision"))).decideurId(rs.getObject("decideur_id", Long.class))
            .dateCreate(fromTs(rs.getTimestamp("datecreate"))).build(), id);
  }

  @Override
  public List<DemandeModifFournisseur> findModifs(int offset, int limit, String statut) {
    StringBuilder sql = new StringBuilder("SELECT * FROM demandes_modif_fournisseur WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (statut != null && !statut.isBlank()) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), (rs, i) -> DemandeModifFournisseur.builder()
        .id(rs.getLong("id")).fkFournisseur(rs.getLong("fk_fournisseur")).statut(rs.getString("statut"))
        .motif(rs.getString("motif")).commentaireDecision(rs.getString("commentaire_decision"))
        .dateDecision(fromTs(rs.getTimestamp("date_decision"))).decideurId(rs.getObject("decideur_id", Long.class))
        .dateCreate(fromTs(rs.getTimestamp("datecreate"))).build(), params.toArray());
  }

  @Override
  public long countModifs(String statut) {
    if (statut != null && !statut.isBlank()) {
      Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demandes_modif_fournisseur WHERE statut = ?", Long.class, statut);
      return c != null ? c : 0L;
    }
    Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demandes_modif_fournisseur", Long.class);
    return c != null ? c : 0L;
  }

  @Override
  public int saveModif(DemandeModifFournisseur d) {
    String sql = "INSERT INTO demandes_modif_fournisseur (fk_fournisseur, statut, motif, datecreate) VALUES (?,?,?,NOW())";
    KeyHolder kh = new GeneratedKeyHolder();
    int rows = jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, d.getFkFournisseur());
      ps.setString(2, d.getStatut() != null ? d.getStatut() : "EN_ATTENTE");
      ps.setString(3, d.getMotif());
      return ps;
    }, kh);
    if (rows > 0) {
      d.setId(extractKey(kh));
    }
    return rows;
  }

  @Override
  public int updateModif(DemandeModifFournisseur d) {
    return update(
        "UPDATE demandes_modif_fournisseur SET statut=?, commentaire_decision=?, date_decision=NOW(), decideur_id=?, dateupdate=NOW() WHERE id=?",
        d.getStatut(), d.getCommentaireDecision(), d.getDecideurId(), d.getId());
  }

  @Override
  public int saveChampModif(ChampModifFournisseur c) {
    return update(
        "INSERT INTO champs_modif_fournisseur (fk_demande_modif, champ, valeur_actuelle, valeur_proposee) VALUES (?,?,?,?)",
        c.getFkDemandeModif(), c.getChamp(), c.getValeurActuelle(), c.getValeurProposee());
  }

  @Override
  public List<ChampModifFournisseur> findChampsModif(Long demandeId) {
    return jdbcTemplate.query(
        "SELECT * FROM champs_modif_fournisseur WHERE fk_demande_modif = ?",
        (rs, i) -> ChampModifFournisseur.builder()
            .id(rs.getLong("id")).fkDemandeModif(rs.getLong("fk_demande_modif")).champ(rs.getString("champ"))
            .valeurActuelle(rs.getString("valeur_actuelle")).valeurProposee(rs.getString("valeur_proposee"))
            .approuve(rs.getObject("approuve") != null ? rs.getBoolean("approuve") : null).build(),
        demandeId);
  }

  @Override
  public int saveDemandeReouverture(Long offreId, String motif) {
    return update(
        "INSERT INTO demandes_reouverture_offre (fk_offre, motif, statut, datecreate) VALUES (?,?, 'EN_ATTENTE', NOW())",
        offreId, motif);
  }

  @Override
  public Optional<Map<String, Object>> findDemandeReouverturePending(Long offreId) {
    List<Map<String, Object>> list = jdbcTemplate.queryForList(
        "SELECT * FROM demandes_reouverture_offre WHERE fk_offre = ? AND statut = 'EN_ATTENTE' ORDER BY id DESC LIMIT 1",
        offreId);
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  @Override
  public List<Map<String, Object>> listReouvertures(String statut, int limit) {
    String sql =
        """
        SELECT d.*, o.id AS offre_id, o.statut AS offre_statut, f.nom AS fournisseur_nom,
               dc.reference AS cotation_reference
        FROM demandes_reouverture_offre d
        INNER JOIN offres_fournisseur o ON o.id = d.fk_offre
        LEFT JOIN fournisseurs f ON f.id = o.fk_fournisseur
        LEFT JOIN demandes_cotation dc ON dc.id = o.fk_demande_cotation
        WHERE (? IS NULL OR d.statut = ?)
        ORDER BY d.datecreate DESC
        LIMIT ?
        """;
    return jdbcTemplate.queryForList(sql, statut, statut, Math.max(limit, 1));
  }

  @Override
  public int updateDemandeReouverture(Long id, String statut, String nouvelleDateLimite, String commentaire, Long decideurId) {
    return update(
        "UPDATE demandes_reouverture_offre SET statut=?, nouvelle_date_limite=?, commentaire_decision=?, date_decision=NOW(), decideur_id=?, dateupdate=NOW() WHERE id=?",
        statut, nouvelleDateLimite, commentaire, decideurId, id);
  }

  @Override
  public int insertMailLog(String idempotenceKey, String destinataire, String sujet, String corps, Long fkInvitation, Long fkDemande) {
    try {
      return update(
          "INSERT INTO cmd_mail_log (idempotence_key, destinataire, sujet, corps, statut, fk_invitation, fk_demande_cotation, datecreate) "
              + "VALUES (?,?,?,?, 'PENDING', ?, ?, NOW())",
          idempotenceKey, destinataire, sujet, corps, fkInvitation, fkDemande);
    } catch (Exception e) {
      return 0; // idempotence conflict
    }
  }

  @Override
  public List<Map<String, Object>> findPendingMails(int limit) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM cmd_mail_log WHERE statut = 'PENDING' ORDER BY datecreate ASC LIMIT ?", limit);
  }

  @Override
  public List<String> listActiveMailingSendEmails() {
    try {
      return jdbcTemplate.query(
          "SELECT mail FROM mailingsend WHERE actif = 1 AND mail IS NOT NULL AND TRIM(mail) <> ''",
          (rs, i) -> rs.getString("mail"));
    } catch (Exception e) {
      log.warn("Impossible de lire mailingsend pour CC commandes: {}", e.getMessage());
      return List.of();
    }
  }

  @Override
  public int markMailSent(Long id) {
    return update("UPDATE cmd_mail_log SET statut='SENT', sent_at=NOW(), dateupdate=NOW() WHERE id=?", id);
  }

  @Override
  public int markMailFailed(Long id, String error) {
    return update("UPDATE cmd_mail_log SET statut='FAILED', attempts=attempts+1, last_error=?, dateupdate=NOW() WHERE id=?",
        error, id);
  }

  @Override
  public int insertOutbox(String eventType, String topic, String eventKey, String payload) {
    return update(
        "INSERT INTO outbox_events (event_type, topic, event_key, event_payload, status, retry_count, created_at) VALUES (?,?,?,?,'PENDING',0,NOW())",
        eventType, topic, eventKey, payload);
  }

  @Override
  public Map<String, Long> dashboardCounts() {
    Map<String, Long> m = new LinkedHashMap<>();
    m.put("cotationsBrouillon", count("SELECT COUNT(*) FROM demandes_cotation WHERE statut='BROUILLON'"));
    m.put("cotationsOuvertes", count("SELECT COUNT(*) FROM demandes_cotation WHERE statut IN ('ENVOYEE','EN_ANALYSE')"));
    m.put("offresEnAttente", count("SELECT COUNT(*) FROM invitations_fournisseur WHERE statut IN ('CREEE','ENVOYEE','OUVERTE','BROUILLON_OFFRE','REOUVERTE')"));
    m.put("offresSoumises", count("SELECT COUNT(*) FROM offres_fournisseur WHERE statut='SOUMISE'"));
    m.put("attributionsEnCours", count("SELECT COUNT(*) FROM demandes_cotation WHERE statut IN ('EN_ANALYSE','ENVOYEE')"));
    m.put("bonsEnCours", count("SELECT COUNT(*) FROM bons_commande WHERE statut IN ('BROUILLON','EN_VALIDATION','VALIDE','ENVOYE','CONFIRME','PARTIELLEMENT_LIVRE')"));
    m.put("bonsEnRetard", count("SELECT COUNT(*) FROM bons_commande WHERE statut='EN_RETARD' OR (date_livraison_prevue < CURDATE() AND statut NOT IN ('TOTALEMENT_LIVRE','CLOTURE','ANNULE'))"));
    m.put("livraisonsPartielles", count("SELECT COUNT(*) FROM bons_commande WHERE statut='PARTIELLEMENT_LIVRE'"));
    m.put("reliquatsOuverts", count("SELECT COUNT(*) FROM lignes_bon_commande WHERE quantite_recue < quantite_commandee"));
    m.put("evaluationsEnAttente", count(
        "SELECT COUNT(*) FROM bons_commande bc WHERE bc.statut IN ('TOTALEMENT_LIVRE','PARTIELLEMENT_LIVRE') "
            + "AND NOT EXISTS (SELECT 1 FROM evaluations_fournisseur ef WHERE ef.fk_bon_commande = bc.id)"));
    m.put("modifsFournisseurEnAttente", count("SELECT COUNT(*) FROM demandes_modif_fournisseur WHERE statut='EN_ATTENTE'"));
    return m;
  }

  private long count(String sql) {
    try {
      Long c = jdbcTemplate.queryForObject(sql, Long.class);
      return c != null ? c : 0L;
    } catch (Exception e) {
      return 0L;
    }
  }

  @Override
  public String findFournisseurNom(Long id) {
    if (id == null) return null;
    try {
      return jdbcTemplate.queryForObject("SELECT nom FROM fournisseurs WHERE id = ?", String.class, id);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String findFournisseurEmail(Long id) {
    if (id == null) return null;
    try {
      return jdbcTemplate.queryForObject("SELECT email FROM fournisseurs WHERE id = ?", String.class, id);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String findPharmacieNom(Long id) {
    if (id == null) return null;
    try {
      return jdbcTemplate.queryForObject("SELECT designation FROM pharmacies WHERE id = ?", String.class, id);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String findProduitNom(Long id) {
    if (id == null) return null;
    try {
      return jdbcTemplate.queryForObject("SELECT nomcommercial FROM produits WHERE id = ?", String.class, id);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Long findStockId(Long fkProduit, Long fkPharmacie) {
    if (fkProduit == null || fkPharmacie == null) return null;
    try {
      return jdbcTemplate.queryForObject(
          "SELECT id FROM stock_produits WHERE fkProduits = ? AND fkPharmacies = ? LIMIT 1",
          Long.class, fkProduit, fkPharmacie);
    } catch (Exception e) {
      return null;
    }
  }
}
