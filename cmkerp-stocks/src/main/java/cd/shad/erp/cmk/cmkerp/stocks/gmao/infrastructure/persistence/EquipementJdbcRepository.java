package cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Categorie;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Criticite;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.EtatGeneral;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Fonctionnement;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Statut;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class EquipementJdbcRepository {

  private static final String WRITE_COLUMNS = """
      code_interne, designation, categorie, marque, modele, numero_serie, fk_site, fk_pharmacie,
      localisation, statut, criticite, date_mise_en_service, date_garantie_fin,
      date_inventaire, nom_inventoriste, etablissement, service, fabricant, pays_acquisition,
      annee_fabrication, date_installation, fournisseur, fournisseur_correspondant,
      fournisseur_telephone, fournisseur_email, fournisseur_adresse, etat_general, fonctionnement,
      contrat_maintenance, contrat_numero, contrat_echeance, maintenance_interne, maintenance_externe,
      frequence_maintenance_jours, derniere_maintenance, prochaine_maintenance,
      technicien_responsable, technicien_contact, consommables_disponibles, pieces_rechange_disponibles,
      manuel_utilisateur, manuel_technique, accessoires_complets, responsable_service,
      ingenieur_biomedical, notes, actif
      """;

  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<Equipement> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

  public Long insert(Equipement e) {
    String sql = """
        INSERT INTO gmao_equipement
        (%s, datecreate, usercreateid)
        VALUES (%s, ?, ?)
        """.formatted(WRITE_COLUMNS, placeholders(47));
    KeyHolder keyHolder = new GeneratedKeyHolder();
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
      int i = bindWrite(ps, e, 1);
      ps.setTimestamp(i++, Timestamp.valueOf(now));
      setNullableLong(ps, i, e.getUserCreateId());
      return ps;
    }, keyHolder);
    Number key = keyHolder.getKey();
    return key != null ? key.longValue() : null;
  }

  public int update(Equipement e) {
    String sql = """
        UPDATE gmao_equipement SET
        """ + setClause(WRITE_COLUMNS) + """
          , dateupdate = ?, userupdateid = ?
        WHERE id = ?
        """;
    return jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql);
      int i = bindWrite(ps, e, 1);
      ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
      setNullableLong(ps, i++, e.getUserUpdateId());
      ps.setLong(i, e.getId());
      return ps;
    });
  }

  public Optional<Equipement> findById(Long id) {
    List<Equipement> rows =
        jdbcTemplate.query("SELECT * FROM gmao_equipement WHERE id = ?", ROW_MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public Optional<Equipement> findByCode(String code) {
    List<Equipement> rows =
        jdbcTemplate.query("SELECT * FROM gmao_equipement WHERE code_interne = ?", ROW_MAPPER, code);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<Equipement> findAll(Long fkPharmacie, String statut, String categorie, String search,
      Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder("SELECT * FROM gmao_equipement WHERE 1=1");
    List<Object> params = new ArrayList<>();
    appendFilters(sql, params, fkPharmacie, statut, categorie, search, actif);
    sql.append(" ORDER BY designation ASC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
  }

  public long count(Long fkPharmacie, String statut, String categorie, String search, Boolean actif) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM gmao_equipement WHERE 1=1");
    List<Object> params = new ArrayList<>();
    appendFilters(sql, params, fkPharmacie, statut, categorie, search, actif);
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0;
  }

  public long countByStatut(String statut) {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_equipement WHERE actif = 1 AND statut = ?", Long.class, statut);
    return c != null ? c : 0;
  }

  public long countActifs() {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_equipement WHERE actif = 1", Long.class);
    return c != null ? c : 0;
  }

  private void appendFilters(StringBuilder sql, List<Object> params, Long fkPharmacie,
      String statut, String categorie, String search, Boolean actif) {
    if (fkPharmacie != null) {
      sql.append(" AND fk_pharmacie = ?");
      params.add(fkPharmacie);
    }
    if (StringUtils.hasText(statut)) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    if (StringUtils.hasText(categorie)) {
      sql.append(" AND categorie = ?");
      params.add(categorie);
    }
    if (actif != null) {
      sql.append(" AND actif = ?");
      params.add(actif ? 1 : 0);
    }
    if (StringUtils.hasText(search)) {
      sql.append("""
           AND (code_interne LIKE ? OR designation LIKE ? OR marque LIKE ?
                OR modele LIKE ? OR numero_serie LIKE ? OR localisation LIKE ?
                OR fabricant LIKE ? OR fournisseur LIKE ? OR service LIKE ? OR etablissement LIKE ?)
          """);
      String like = "%" + search.trim() + "%";
      for (int i = 0; i < 10; i++) {
        params.add(like);
      }
    }
  }

  private int bindWrite(PreparedStatement ps, Equipement e, int startIndex) throws SQLException {
    int i = startIndex;
    ps.setString(i++, e.getCodeInterne());
    ps.setString(i++, e.getDesignation());
    ps.setString(i++, e.getCategorie().name());
    ps.setString(i++, e.getMarque());
    ps.setString(i++, e.getModele());
    ps.setString(i++, e.getNumeroSerie());
    setNullableLong(ps, i++, e.getFkSite());
    setNullableLong(ps, i++, e.getFkPharmacie());
    ps.setString(i++, e.getLocalisation());
    ps.setString(i++, e.getStatut().name());
    ps.setString(i++, e.getCriticite().name());
    setNullableDate(ps, i++, e.getDateMiseEnService());
    setNullableDate(ps, i++, e.getDateGarantieFin());
    setNullableDate(ps, i++, e.getDateInventaire());
    ps.setString(i++, e.getNomInventoriste());
    ps.setString(i++, e.getEtablissement());
    ps.setString(i++, e.getService());
    ps.setString(i++, e.getFabricant());
    ps.setString(i++, e.getPaysAcquisition());
    setNullableInt(ps, i++, e.getAnneeFabrication());
    setNullableDate(ps, i++, e.getDateInstallation());
    ps.setString(i++, e.getFournisseur());
    ps.setString(i++, e.getFournisseurCorrespondant());
    ps.setString(i++, e.getFournisseurTelephone());
    ps.setString(i++, e.getFournisseurEmail());
    ps.setString(i++, e.getFournisseurAdresse());
    setNullableEnum(ps, i++, e.getEtatGeneral());
    setNullableEnum(ps, i++, e.getFonctionnement());
    ps.setInt(i++, e.isContratMaintenance() ? 1 : 0);
    ps.setString(i++, e.getContratNumero());
    setNullableDate(ps, i++, e.getContratEcheance());
    ps.setInt(i++, e.isMaintenanceInterne() ? 1 : 0);
    ps.setInt(i++, e.isMaintenanceExterne() ? 1 : 0);
    setNullableInt(ps, i++, e.getFrequenceMaintenanceJours());
    setNullableDate(ps, i++, e.getDerniereMaintenance());
    setNullableDate(ps, i++, e.getProchaineMaintenance());
    ps.setString(i++, e.getTechnicienResponsable());
    ps.setString(i++, e.getTechnicienContact());
    ps.setInt(i++, e.isConsommablesDisponibles() ? 1 : 0);
    ps.setInt(i++, e.isPiecesRechangeDisponibles() ? 1 : 0);
    ps.setInt(i++, e.isManuelUtilisateur() ? 1 : 0);
    ps.setInt(i++, e.isManuelTechnique() ? 1 : 0);
    ps.setInt(i++, e.isAccessoiresComplets() ? 1 : 0);
    ps.setString(i++, e.getResponsableService());
    ps.setString(i++, e.getIngenieurBiomedical());
    ps.setString(i++, e.getNotes());
    ps.setInt(i++, e.isActif() ? 1 : 0);
    return i;
  }

  private static Equipement mapRow(ResultSet rs) throws SQLException {
    return Equipement.builder()
        .id(rs.getLong("id"))
        .codeInterne(rs.getString("code_interne"))
        .designation(rs.getString("designation"))
        .categorie(Categorie.valueOf(rs.getString("categorie")))
        .marque(rs.getString("marque"))
        .modele(rs.getString("modele"))
        .numeroSerie(rs.getString("numero_serie"))
        .fkSite(getLong(rs, "fk_site"))
        .fkPharmacie(getLong(rs, "fk_pharmacie"))
        .localisation(rs.getString("localisation"))
        .statut(Statut.valueOf(rs.getString("statut")))
        .criticite(Criticite.valueOf(rs.getString("criticite")))
        .dateMiseEnService(toLocalDate(rs.getDate("date_mise_en_service")))
        .dateGarantieFin(toLocalDate(rs.getDate("date_garantie_fin")))
        .dateInventaire(toLocalDate(getDate(rs, "date_inventaire")))
        .nomInventoriste(getString(rs, "nom_inventoriste"))
        .etablissement(getString(rs, "etablissement"))
        .service(getString(rs, "service"))
        .fabricant(getString(rs, "fabricant"))
        .paysAcquisition(getString(rs, "pays_acquisition"))
        .anneeFabrication(getInteger(rs, "annee_fabrication"))
        .dateInstallation(toLocalDate(getDate(rs, "date_installation")))
        .fournisseur(getString(rs, "fournisseur"))
        .fournisseurCorrespondant(getString(rs, "fournisseur_correspondant"))
        .fournisseurTelephone(getString(rs, "fournisseur_telephone"))
        .fournisseurEmail(getString(rs, "fournisseur_email"))
        .fournisseurAdresse(getString(rs, "fournisseur_adresse"))
        .etatGeneral(parseEnum(rs, "etat_general", EtatGeneral.class))
        .fonctionnement(parseEnum(rs, "fonctionnement", Fonctionnement.class))
        .contratMaintenance(getBoolean(rs, "contrat_maintenance"))
        .contratNumero(getString(rs, "contrat_numero"))
        .contratEcheance(toLocalDate(getDate(rs, "contrat_echeance")))
        .maintenanceInterne(getBoolean(rs, "maintenance_interne"))
        .maintenanceExterne(getBoolean(rs, "maintenance_externe"))
        .frequenceMaintenanceJours(getInteger(rs, "frequence_maintenance_jours"))
        .derniereMaintenance(toLocalDate(getDate(rs, "derniere_maintenance")))
        .prochaineMaintenance(toLocalDate(getDate(rs, "prochaine_maintenance")))
        .technicienResponsable(getString(rs, "technicien_responsable"))
        .technicienContact(getString(rs, "technicien_contact"))
        .consommablesDisponibles(getBoolean(rs, "consommables_disponibles"))
        .piecesRechangeDisponibles(getBoolean(rs, "pieces_rechange_disponibles"))
        .manuelUtilisateur(getBoolean(rs, "manuel_utilisateur"))
        .manuelTechnique(getBoolean(rs, "manuel_technique"))
        .accessoiresComplets(getBoolean(rs, "accessoires_complets"))
        .responsableService(getString(rs, "responsable_service"))
        .ingenieurBiomedical(getString(rs, "ingenieur_biomedical"))
        .notes(rs.getString("notes"))
        .actif(rs.getBoolean("actif"))
        .dateCreate(toLocalDateTime(rs.getTimestamp("datecreate")))
        .dateUpdate(toLocalDateTime(rs.getTimestamp("dateupdate")))
        .userCreateId(getLong(rs, "usercreateid"))
        .userUpdateId(getLong(rs, "userupdateid"))
        .build();
  }

  private static String placeholders(int count) {
    return String.join(", ", java.util.Collections.nCopies(count, "?"));
  }

  private static String setClause(String columns) {
    String[] parts = columns.trim().split("\\s*,\\s*");
    StringBuilder sb = new StringBuilder();
    for (int idx = 0; idx < parts.length; idx++) {
      if (idx > 0) {
        sb.append(", ");
      }
      sb.append(parts[idx]).append(" = ?");
    }
    return sb.toString();
  }

  private static void setNullableLong(PreparedStatement ps, int index, Long value)
      throws SQLException {
    if (value != null) {
      ps.setLong(index, value);
    } else {
      ps.setNull(index, Types.BIGINT);
    }
  }

  private static void setNullableInt(PreparedStatement ps, int index, Integer value)
      throws SQLException {
    if (value != null) {
      ps.setInt(index, value);
    } else {
      ps.setNull(index, Types.INTEGER);
    }
  }

  private static void setNullableDate(PreparedStatement ps, int index, LocalDate value)
      throws SQLException {
    if (value != null) {
      ps.setDate(index, Date.valueOf(value));
    } else {
      ps.setNull(index, Types.DATE);
    }
  }

  private static void setNullableEnum(PreparedStatement ps, int index, Enum<?> value)
      throws SQLException {
    if (value != null) {
      ps.setString(index, value.name());
    } else {
      ps.setNull(index, Types.VARCHAR);
    }
  }

  private static Long getLong(ResultSet rs, String col) throws SQLException {
    long v = rs.getLong(col);
    return rs.wasNull() ? null : v;
  }

  private static Integer getInteger(ResultSet rs, String col) throws SQLException {
    int v = rs.getInt(col);
    return rs.wasNull() ? null : v;
  }

  private static String getString(ResultSet rs, String col) throws SQLException {
    try {
      return rs.getString(col);
    } catch (SQLException ex) {
      return null;
    }
  }

  private static Date getDate(ResultSet rs, String col) throws SQLException {
    try {
      return rs.getDate(col);
    } catch (SQLException ex) {
      return null;
    }
  }

  private static boolean getBoolean(ResultSet rs, String col) throws SQLException {
    try {
      return rs.getBoolean(col);
    } catch (SQLException ex) {
      return false;
    }
  }

  private static <E extends Enum<E>> E parseEnum(ResultSet rs, String col, Class<E> type)
      throws SQLException {
    String value = getString(rs, col);
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Enum.valueOf(type, value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static LocalDate toLocalDate(Date d) {
    return d != null ? d.toLocalDate() : null;
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime() : null;
  }
}
