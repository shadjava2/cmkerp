package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Accès MySQL aux tables métier Facturation normalisées (V32).
 * Source de vérité pour l'affichage et les FK ; {@code erp_clinic_referentials} reste pont d'import.
 */
@Repository
public class ErpFacturationRelationalStore {

  private final JdbcTemplate jdbc;

  public ErpFacturationRelationalStore(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean supports(String referentialCode) {
    if (referentialCode == null) {
      return false;
    }
    return switch (referentialCode.trim().toUpperCase(Locale.ROOT)) {
      case "SERVICES", "IMPUTATIONS", "LOCATIONS", "MEDECINS", "SOCIETES", "CLASSES", "TARIFS",
          "AYANTS_DROIT" -> true;
      default -> false;
    };
  }

  public List<Map<String, Object>> search(String referentialCode, String q, Boolean actif, int limit, int offset) {
    return switch (referentialCode.trim().toUpperCase(Locale.ROOT)) {
      case "SERVICES" -> searchServices(q, actif, limit, offset);
      case "IMPUTATIONS" -> searchImputations(q, actif, limit, offset);
      case "LOCATIONS" -> searchLocations(q, actif, limit, offset);
      case "MEDECINS" -> searchMedecins(q, actif, limit, offset);
      case "SOCIETES" -> searchSocietes(q, actif, limit, offset);
      case "CLASSES" -> searchClasses(q, actif, limit, offset);
      case "TARIFS" -> searchTarifs(q, actif, limit, offset);
      case "AYANTS_DROIT" -> searchAyants(q, actif, limit, offset);
      default -> List.of();
    };
  }

  public long count(String referentialCode, String q, Boolean actif) {
    return switch (referentialCode.trim().toUpperCase(Locale.ROOT)) {
      case "SERVICES" -> countSimple("erp_services", "code", "libelle", q, actif);
      case "IMPUTATIONS" -> countSimple("erp_imputations", "code", "designation", q, actif);
      case "LOCATIONS" -> countSimple("erp_locations", "code", "libelle", q, actif);
      case "MEDECINS" -> countSimple("erp_medecins", "code", "nom", q, actif);
      case "SOCIETES" -> countSocietes(q, actif);
      case "CLASSES" -> countClasses(q, actif);
      case "TARIFS" -> countTarifs(q, actif);
      case "AYANTS_DROIT" -> countAyants(q, actif);
      default -> 0L;
    };
  }

  public Optional<Map<String, Object>> findById(String referentialCode, long id) {
    List<Map<String, Object>> rows = searchById(referentialCode, id);
    return rows.stream().findFirst();
  }

  public void upsertSocieteFromImport(String code, String designation, String adresse, String nImpot,
      String nrc, String idNat, String contratCode, String fingerprint) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return;
    }
    Long contratId = findContratId(contratCode);
    int updated = jdbc.update(
        """
            UPDATE erp_societes
            SET designation = ?, adresse = ?, n_impot = ?, nrc = ?, id_nat = ?,
                legacy_contrat_code = ?, fk_contrat = COALESCE(?, fk_contrat),
                sync_status = 'IMPORTED', sync_fingerprint = ?, last_synced_at = NOW(),
                source_system = 'IMPORT_CLINIQUE', dateupdate = NOW()
            WHERE code = ?
            """,
        designation, adresse, nImpot, nrc, idNat, trim(contratCode), contratId, fingerprint, c);
    if (updated == 0) {
      jdbc.update(
          """
              INSERT INTO erp_societes
                (code, designation, adresse, n_impot, nrc, id_nat, fk_contrat, legacy_contrat_code,
                 actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', ?, NOW())
              """,
          c, designation, adresse, nImpot, nrc, idNat, contratId, trim(contratCode), c, fingerprint);
    }
  }

  public void upsertAyantFromImport(String matricule, String nom, String sexe, LocalDate dateNais,
      String nationalite, String relation, String codeFam, String codeSoc, String contrat,
      String assurance, String covid, String location, String source, String fingerprint) {
    String m = trim(matricule);
    if (m == null || m.isBlank()) {
      return;
    }
    Long societeId = findSocieteId(codeSoc);
    Long contratId = findContratId(contrat);
    Long locationId = findLocationId(location);
    int updated = jdbc.update(
        """
            UPDATE erp_ayants_droit
            SET nom_complet = ?, sexe = ?, date_naissance = ?, nationalite = ?, relation = ?,
                code_famille = ?, legacy_codesoc = ?, fk_societe = COALESCE(?, fk_societe),
                legacy_contrat_code = ?, fk_contrat = COALESCE(?, fk_contrat),
                assurance = ?, covid = ?, legacy_location_code = ?, fk_location = COALESCE(?, fk_location),
                source_legacy = ?, sync_status = 'IMPORTED', sync_fingerprint = ?,
                last_synced_at = NOW(), source_system = 'IMPORT_CLINIQUE', dateupdate = NOW()
            WHERE matricule = ?
            """,
        nom, sexe, dateNais == null ? null : Date.valueOf(dateNais), nationalite, relation, codeFam,
        trim(codeSoc), societeId, trim(contrat), contratId, assurance, covid, trim(location), locationId,
        source, fingerprint, m);
    if (updated == 0) {
      jdbc.update(
          """
              INSERT INTO erp_ayants_droit
                (matricule, nom_complet, sexe, date_naissance, nationalite, relation, code_famille,
                 fk_societe, legacy_codesoc, fk_contrat, legacy_contrat_code, assurance, covid,
                 legacy_location_code, fk_location, source_legacy, actif, source_system, legacy_code,
                 sync_status, sync_fingerprint, last_synced_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', ?, NOW())
              """,
          m, nom, sexe, dateNais == null ? null : Date.valueOf(dateNais), nationalite, relation, codeFam,
          societeId, trim(codeSoc), contratId, trim(contrat), assurance, covid, trim(location), locationId,
          source, m, fingerprint);
    }
  }

  public void upsertServiceFromImport(String code, String libelle, String fingerprint) {
    upsertSimple("erp_services", "libelle", code, libelle, fingerprint);
  }

  public void upsertImputationFromImport(String code, String designation, String fingerprint) {
    upsertSimple("erp_imputations", "designation", code, designation, fingerprint);
  }

  public void upsertLocationFromImport(String code, String libelle, String adresse, Long siteId, String fingerprint) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return;
    }
    int updated = jdbc.update(
        """
            UPDATE erp_locations
            SET libelle = ?, adresse = ?, fk_site = COALESCE(?, fk_site),
                sync_status = 'IMPORTED', sync_fingerprint = ?, last_synced_at = NOW(),
                source_system = 'IMPORT_CLINIQUE', dateupdate = NOW()
            WHERE code = ?
            """,
        libelle, adresse, siteId, fingerprint, c);
    if (updated == 0) {
      jdbc.update(
          """
              INSERT INTO erp_locations
                (code, libelle, adresse, fk_site, actif, source_system, legacy_code,
                 sync_status, sync_fingerprint, last_synced_at)
              VALUES (?, ?, ?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', ?, NOW())
              """,
          c, libelle, adresse, siteId, c, fingerprint);
    }
  }

  public void upsertMedecinFromImport(String code, String nom, String fingerprint) {
    upsertSimple("erp_medecins", "nom", code, nom, fingerprint);
  }

  public void upsertClasseFromImport(String code, String libelle, String serviceText, String fingerprint) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return;
    }
    Long serviceId = resolveServiceId(serviceText);
    int updated = jdbc.update(
        """
            UPDATE erp_classes
            SET libelle = ?, legacy_service_text = ?, fk_service = COALESCE(?, fk_service),
                sync_status = 'IMPORTED', sync_fingerprint = ?, last_synced_at = NOW(),
                source_system = 'IMPORT_CLINIQUE', dateupdate = NOW()
            WHERE code = ?
            """,
        libelle, serviceText, serviceId, fingerprint, c);
    if (updated == 0) {
      jdbc.update(
          """
              INSERT INTO erp_classes
                (code, libelle, legacy_service_text, fk_service, actif, source_system, legacy_code,
                 sync_status, sync_fingerprint, last_synced_at)
              VALUES (?, ?, ?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', ?, NOW())
              """,
          c, libelle, serviceText, serviceId, c, fingerprint);
    }
  }

  public void upsertTarifFromImport(String code, String designation, String serviceCode, String classeCode,
      String imputationCode, BigDecimal montant, BigDecimal montantPrive, String fingerprint) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return;
    }
    Long serviceId = findServiceId(serviceCode);
    Long classeId = findClasseId(classeCode);
    Long imputationId = findImputationId(imputationCode);
    int updated = jdbc.update(
        """
            UPDATE erp_tarifs
            SET designation = ?, legacy_service_code = ?, fk_service = COALESCE(?, fk_service),
                legacy_classe_code = ?, fk_classe = COALESCE(?, fk_classe),
                legacy_imputation_code = ?, fk_imputation = COALESCE(?, fk_imputation),
                montant = ?, montant_prive = ?,
                sync_status = 'IMPORTED', sync_fingerprint = ?, last_synced_at = NOW(),
                source_system = 'IMPORT_CLINIQUE', dateupdate = NOW()
            WHERE code = ?
            """,
        designation, trim(serviceCode), serviceId, trim(classeCode), classeId,
        trim(imputationCode), imputationId, montant, montantPrive, fingerprint, c);
    if (updated == 0) {
      jdbc.update(
          """
              INSERT INTO erp_tarifs
                (code, designation, fk_service, legacy_service_code, fk_classe, legacy_classe_code,
                 fk_imputation, legacy_imputation_code, montant, montant_prive, actif,
                 source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', ?, NOW())
              """,
          c, designation, serviceId, trim(serviceCode), classeId, trim(classeCode),
          imputationId, trim(imputationCode), montant, montantPrive, c, fingerprint);
    }
  }

  public long insertSociete(String code, String designation, String adresse, String nImpot, String nrc,
      String idNat, Long contratId, String legacyContrat, Long userId) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      var ps = con.prepareStatement(
          """
              INSERT INTO erp_societes
                (code, designation, adresse, n_impot, nrc, id_nat, fk_contrat, legacy_contrat_code,
                 actif, source_system, legacy_code, sync_status, usercreateid, userupdateid)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 'OPERATION_ERP', ?, 'LOCAL_ONLY', ?, ?)
              """,
          new String[] {"id"});
      ps.setString(1, trim(code));
      ps.setString(2, designation);
      ps.setString(3, adresse);
      ps.setString(4, nImpot);
      ps.setString(5, nrc);
      ps.setString(6, idNat);
      if (contratId == null) {
        ps.setObject(7, null);
      } else {
        ps.setLong(7, contratId);
      }
      ps.setString(8, trim(legacyContrat));
      ps.setString(9, trim(code));
      if (userId == null) {
        ps.setObject(10, null);
        ps.setObject(11, null);
      } else {
        ps.setLong(10, userId);
        ps.setLong(11, userId);
      }
      return ps;
    }, keys);
    Number key = keys.getKey();
    return key == null ? 0L : key.longValue();
  }

  public int updateSociete(long id, String designation, String adresse, String nImpot, String nrc,
      String idNat, Long contratId, String legacyContrat, int version, Long userId) {
    return jdbc.update(
        """
            UPDATE erp_societes
            SET designation = ?, adresse = ?, n_impot = ?, nrc = ?, id_nat = ?,
                fk_contrat = ?, legacy_contrat_code = ?, version = version + 1,
                userupdateid = ?, dateupdate = NOW(), sync_status = 'LOCAL_ONLY'
            WHERE id = ? AND version = ?
            """,
        designation, adresse, nImpot, nrc, idNat, contratId, trim(legacyContrat), userId, id, version);
  }

  public int updateActif(String referentialCode, long id, boolean actif, int version, Long userId) {
    String table = tableName(referentialCode);
    if (table == null) {
      return 0;
    }
    return jdbc.update(
        "UPDATE " + table
            + " SET actif = ?, version = version + 1, userupdateid = ?, dateupdate = NOW() WHERE id = ? AND version = ?",
        actif ? 1 : 0,
        userId,
        id,
        version);
  }

  private static String tableName(String referentialCode) {
    if (referentialCode == null) {
      return null;
    }
    return switch (referentialCode.trim().toUpperCase(Locale.ROOT)) {
      case "SERVICES" -> "erp_services";
      case "IMPUTATIONS" -> "erp_imputations";
      case "LOCATIONS" -> "erp_locations";
      case "MEDECINS" -> "erp_medecins";
      case "SOCIETES" -> "erp_societes";
      case "CLASSES" -> "erp_classes";
      case "TARIFS" -> "erp_tarifs";
      case "AYANTS_DROIT" -> "erp_ayants_droit";
      default -> null;
    };
  }

  public Optional<String> findContratCode(Long contratId) {
    if (contratId == null) {
      return Optional.empty();
    }
    var list = jdbc.query("SELECT code FROM erp_contrats WHERE id = ?", (rs, i) -> rs.getString(1), contratId);
    return list.stream().findFirst().map(this::trim);
  }

  public Long findContratId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_contrats WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  public Long findSocieteId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_societes WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  private List<Map<String, Object>> searchById(String code, long id) {
    return switch (code.trim().toUpperCase(Locale.ROOT)) {
      case "SOCIETES" -> searchSocietesById(id);
      case "AYANTS_DROIT" -> searchAyantsById(id);
      case "TARIFS" -> jdbc.query(
              """
                  SELECT t.id, t.code, t.designation, t.montant, t.montant_prive,
                         t.fk_service, t.legacy_service_code, s.libelle AS service_libelle, s.code AS service_code,
                         t.fk_classe, t.legacy_classe_code, cl.libelle AS classe_libelle,
                         t.fk_imputation, t.legacy_imputation_code, i.designation AS imputation_designation,
                         t.actif, t.source_system, t.legacy_code, t.sync_status, t.version, t.last_synced_at
                  FROM erp_tarifs t
                  LEFT JOIN erp_services s ON s.id = t.fk_service
                  LEFT JOIN erp_classes cl ON cl.id = t.fk_classe
                  LEFT JOIN erp_imputations i ON i.id = t.fk_imputation
                  WHERE t.id = ?
                  """,
              (rs, rowNum) -> {
                Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("designation"));
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("CODEPRES", rs.getString("code"));
                fields.put("LIBPRES", rs.getString("designation"));
                fields.put("CODESERV", firstNonBlank(rs.getString("service_code"), rs.getString("legacy_service_code")));
                fields.put("serviceDesignation", rs.getString("service_libelle"));
                fields.put("CLASSE", firstNonBlank(rs.getString("classe_libelle"), rs.getString("legacy_classe_code")));
                fields.put("MONTANT", rs.getBigDecimal("montant"));
                fields.put("MONTPRIVE", rs.getBigDecimal("montant_prive"));
                fields.put("IMPUTATION", rs.getString("legacy_imputation_code"));
                fields.put("imputationDesignation", rs.getString("imputation_designation"));
                item.put("fields", fields);
                return item;
              },
              id);
      case "SERVICES", "IMPUTATIONS", "LOCATIONS", "MEDECINS", "CLASSES" ->
          search(code, null, null, 500, 0).stream()
              .filter(row -> row.get("id") instanceof Number n && n.longValue() == id)
              .limit(1)
              .toList();
      default -> List.of();
    };
  }

  private List<Map<String, Object>> searchSocietesById(long id) {
    return jdbc.query(
        """
            SELECT s.id, s.code, s.designation, s.adresse, s.n_impot, s.nrc, s.id_nat,
                   s.fk_contrat, s.legacy_contrat_code, c.code AS contrat_code, c.designation AS contrat_designation,
                   s.actif, s.source_system, s.legacy_code, s.sync_status, s.version, s.last_synced_at
            FROM erp_societes s
            LEFT JOIN erp_contrats c ON c.id = s.fk_contrat
            WHERE s.id = ?
            """,
        (rs, i) -> mapSociete(rs), id);
  }

  private List<Map<String, Object>> searchAyantsById(long id) {
    return jdbc.query(
        """
            SELECT a.id, a.matricule, a.nom_complet, a.sexe, a.date_naissance, a.relation, a.code_famille,
                   a.fk_societe, a.legacy_codesoc, soc.designation AS societe_designation,
                   a.fk_contrat, a.legacy_contrat_code, ctr.code AS contrat_code, ctr.designation AS contrat_designation,
                   a.mediline_ipp, a.actif, a.source_system, a.legacy_code, a.sync_status, a.version, a.last_synced_at
            FROM erp_ayants_droit a
            LEFT JOIN erp_societes soc ON soc.id = a.fk_societe
            LEFT JOIN erp_contrats ctr ON ctr.id = a.fk_contrat
            WHERE a.id = ?
            """,
        (rs, i) -> mapAyant(rs), id);
  }

  private List<Map<String, Object>> searchSocietes(String q, Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT s.id, s.code, s.designation, s.adresse, s.n_impot, s.nrc, s.id_nat,
                   s.fk_contrat, s.legacy_contrat_code, c.code AS contrat_code, c.designation AS contrat_designation,
                   s.actif, s.source_system, s.legacy_code, s.sync_status, s.version, s.last_synced_at
            FROM erp_societes s
            LEFT JOIN erp_contrats c ON c.id = s.fk_contrat
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "s.code", "s.designation", "c.designation", "c.code");
    appendActif(sql, args, actif, "s.actif");
    sql.append(" ORDER BY s.code ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> mapSociete(rs), args.toArray());
  }

  private long countSocietes(String q, Boolean actif) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT COUNT(*) FROM erp_societes s
            LEFT JOIN erp_contrats c ON c.id = s.fk_contrat
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "s.code", "s.designation", "c.designation", "c.code");
    appendActif(sql, args, actif, "s.actif");
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  private List<Map<String, Object>> searchAyants(String q, Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT a.id, a.matricule, a.nom_complet, a.sexe, a.date_naissance, a.relation, a.code_famille,
                   a.fk_societe, a.legacy_codesoc, soc.designation AS societe_designation,
                   a.fk_contrat, a.legacy_contrat_code, ctr.code AS contrat_code, ctr.designation AS contrat_designation,
                   a.mediline_ipp, a.actif, a.source_system, a.legacy_code, a.sync_status, a.version, a.last_synced_at
            FROM erp_ayants_droit a
            LEFT JOIN erp_societes soc ON soc.id = a.fk_societe
            LEFT JOIN erp_contrats ctr ON ctr.id = a.fk_contrat
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q,
        "a.matricule", "a.nom_complet", "a.mediline_ipp", "soc.designation", "soc.code",
        "ctr.designation", "ctr.code", "a.legacy_codesoc");
    appendActif(sql, args, actif, "a.actif");
    sql.append(" ORDER BY a.matricule ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> mapAyant(rs), args.toArray());
  }

  private long countAyants(String q, Boolean actif) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT COUNT(*) FROM erp_ayants_droit a
            LEFT JOIN erp_societes soc ON soc.id = a.fk_societe
            LEFT JOIN erp_contrats ctr ON ctr.id = a.fk_contrat
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q,
        "a.matricule", "a.nom_complet", "a.mediline_ipp", "soc.designation", "soc.code",
        "ctr.designation", "ctr.code", "a.legacy_codesoc");
    appendActif(sql, args, actif, "a.actif");
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  private List<Map<String, Object>> searchServices(String q, Boolean actif, int limit, int offset) {
    return searchNamed("erp_services", "libelle", q, actif, limit, offset, (rs, i) -> {
      Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("libelle"));
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("CODE", rs.getString("code"));
      fields.put("LIBELLE", rs.getString("libelle"));
      item.put("fields", fields);
      return item;
    });
  }

  private List<Map<String, Object>> searchImputations(String q, Boolean actif, int limit, int offset) {
    return searchNamed("erp_imputations", "designation", q, actif, limit, offset, (rs, i) -> {
      Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("designation"));
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("CODE", rs.getString("code"));
      fields.put("DESIGNATION", rs.getString("designation"));
      item.put("fields", fields);
      return item;
    });
  }

  private List<Map<String, Object>> searchLocations(String q, Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT id, code, libelle, adresse, fk_site, actif, source_system, legacy_code,
                   sync_status, version, last_synced_at
            FROM erp_locations WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "code", "libelle", "adresse");
    appendActif(sql, args, actif, "actif");
    sql.append(" ORDER BY code ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("libelle"));
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("CODE", rs.getString("code"));
      fields.put("LIBELLE", rs.getString("libelle"));
      fields.put("ADRESSE", rs.getString("adresse"));
      Object siteId = rs.getObject("fk_site");
      if (siteId != null) {
        fields.put("siteId", ((Number) siteId).longValue());
      }
      item.put("fields", fields);
      return item;
    }, args.toArray());
  }

  private List<Map<String, Object>> searchMedecins(String q, Boolean actif, int limit, int offset) {
    return searchNamed("erp_medecins", "nom", q, actif, limit, offset, (rs, i) -> {
      Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("nom"));
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("CODEMED", rs.getString("code"));
      fields.put("NOM", rs.getString("nom"));
      item.put("fields", fields);
      return item;
    });
  }

  private List<Map<String, Object>> searchClasses(String q, Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT c.id, c.code, c.libelle, c.legacy_service_text, c.fk_service,
                   s.code AS service_code, s.libelle AS service_libelle,
                   c.actif, c.source_system, c.legacy_code, c.sync_status, c.version, c.last_synced_at
            FROM erp_classes c
            LEFT JOIN erp_services s ON s.id = c.fk_service
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "c.code", "c.libelle", "c.legacy_service_text", "s.libelle");
    appendActif(sql, args, actif, "c.actif");
    sql.append(" ORDER BY c.code ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("libelle"));
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("CODE", rs.getString("code"));
      fields.put("LIBELLE", rs.getString("libelle"));
      String serviceLabel = firstNonBlank(rs.getString("service_libelle"), rs.getString("legacy_service_text"));
      fields.put("SERVICE", serviceLabel);
      fields.put("serviceCode", rs.getString("service_code"));
      fields.put("serviceDesignation", rs.getString("service_libelle"));
      fields.put("serviceId", rs.getObject("fk_service"));
      item.put("fields", fields);
      return item;
    }, args.toArray());
  }

  private long countClasses(String q, Boolean actif) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT COUNT(*) FROM erp_classes c
            LEFT JOIN erp_services s ON s.id = c.fk_service
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "c.code", "c.libelle", "c.legacy_service_text", "s.libelle");
    appendActif(sql, args, actif, "c.actif");
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  private List<Map<String, Object>> searchTarifs(String q, Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT t.id, t.code, t.designation, t.montant, t.montant_prive,
                   t.fk_service, t.legacy_service_code, s.libelle AS service_libelle, s.code AS service_code,
                   t.fk_classe, t.legacy_classe_code, cl.libelle AS classe_libelle,
                   t.fk_imputation, t.legacy_imputation_code, i.designation AS imputation_designation,
                   t.actif, t.source_system, t.legacy_code, t.sync_status, t.version, t.last_synced_at
            FROM erp_tarifs t
            LEFT JOIN erp_services s ON s.id = t.fk_service
            LEFT JOIN erp_classes cl ON cl.id = t.fk_classe
            LEFT JOIN erp_imputations i ON i.id = t.fk_imputation
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "t.code", "t.designation", "s.libelle", "i.designation", "i.code");
    appendActif(sql, args, actif, "t.actif");
    sql.append(" ORDER BY t.code ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("designation"));
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("CODEPRES", rs.getString("code"));
      fields.put("LIBPRES", rs.getString("designation"));
      fields.put("CODESERV", firstNonBlank(rs.getString("service_code"), rs.getString("legacy_service_code")));
      fields.put("serviceDesignation", rs.getString("service_libelle"));
      fields.put("serviceId", rs.getObject("fk_service"));
      fields.put("CLASSE", firstNonBlank(rs.getString("classe_libelle"), rs.getString("legacy_classe_code")));
      fields.put("classeDesignation", rs.getString("classe_libelle"));
      fields.put("MONTANT", rs.getBigDecimal("montant"));
      fields.put("MONTPRIVE", rs.getBigDecimal("montant_prive"));
      fields.put("IMPUTATION", firstNonBlank(rs.getString("legacy_imputation_code"), null));
      fields.put("imputationDesignation", rs.getString("imputation_designation"));
      fields.put("imputationId", rs.getObject("fk_imputation"));
      item.put("fields", fields);
      return item;
    }, args.toArray());
  }

  private long countTarifs(String q, Boolean actif) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT COUNT(*) FROM erp_tarifs t
            LEFT JOIN erp_services s ON s.id = t.fk_service
            LEFT JOIN erp_imputations i ON i.id = t.fk_imputation
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "t.code", "t.designation", "s.libelle", "i.designation", "i.code");
    appendActif(sql, args, actif, "t.actif");
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  private List<Map<String, Object>> searchNamed(
      String table, String labelCol, String q, Boolean actif, int limit, int offset,
      org.springframework.jdbc.core.RowMapper<Map<String, Object>> mapper) {
    StringBuilder sql = new StringBuilder(
        "SELECT id, code, " + labelCol + " AS label_col, actif, source_system, legacy_code, sync_status, version, last_synced_at FROM "
            + table + " WHERE 1=1");
    // rebuild with concrete columns for services/imputations/medecins
    if ("erp_services".equals(table)) {
      sql = new StringBuilder(
          "SELECT id, code, libelle, actif, source_system, legacy_code, sync_status, version, last_synced_at FROM erp_services WHERE 1=1");
    } else if ("erp_imputations".equals(table)) {
      sql = new StringBuilder(
          "SELECT id, code, designation, actif, source_system, legacy_code, sync_status, version, last_synced_at FROM erp_imputations WHERE 1=1");
    } else if ("erp_medecins".equals(table)) {
      sql = new StringBuilder(
          "SELECT id, code, nom, actif, source_system, legacy_code, sync_status, version, last_synced_at FROM erp_medecins WHERE 1=1");
    }
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, "code", labelCol.equals("libelle") ? "libelle" : labelCol.equals("nom") ? "nom" : "designation");
    appendActif(sql, args, actif, "actif");
    sql.append(" ORDER BY code ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), mapper, args.toArray());
  }

  private long countSimple(String table, String codeCol, String labelCol, String q, Boolean actif) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + table + " WHERE 1=1");
    var args = new ArrayList<Object>();
    appendTextFilter(sql, args, q, codeCol, labelCol);
    appendActif(sql, args, actif, "actif");
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  private void upsertSimple(String table, String labelCol, String code, String label, String fingerprint) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return;
    }
    int updated = jdbc.update(
        "UPDATE " + table
            + " SET " + labelCol + " = ?, sync_status = 'IMPORTED', sync_fingerprint = ?, last_synced_at = NOW(),"
            + " source_system = 'IMPORT_CLINIQUE', dateupdate = NOW() WHERE code = ?",
        label, fingerprint, c);
    if (updated == 0) {
      jdbc.update(
          "INSERT INTO " + table
              + " (code, " + labelCol + ", actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at)"
              + " VALUES (?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', ?, NOW())",
          c, label, c, fingerprint);
    }
  }

  private Long resolveServiceId(String serviceText) {
    Long byCode = findServiceId(serviceText);
    if (byCode != null) {
      return byCode;
    }
    String t = trim(serviceText);
    if (t == null || t.isBlank()) {
      return null;
    }
    var list = jdbc.query(
        "SELECT id FROM erp_services WHERE UPPER(TRIM(libelle)) = UPPER(TRIM(?)) LIMIT 1",
        (rs, i) -> rs.getLong(1),
        t);
    return list.stream().findFirst().orElse(null);
  }

  private Long findServiceId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_services WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  private Long findClasseId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_classes WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  private Long findImputationId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_imputations WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  private Long findLocationId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_locations WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  private Map<String, Object> mapSociete(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> item = baseItem(rs, rs.getString("code"), rs.getString("designation"));
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("CODESOC", rs.getString("code"));
    fields.put("NOMSOC", rs.getString("designation"));
    fields.put("ADRESSE", rs.getString("adresse"));
    fields.put("N_IMPOT", rs.getString("n_impot"));
    fields.put("NRC", rs.getString("nrc"));
    fields.put("ID_NAT", rs.getString("id_nat"));
    fields.put("CONTRAT", firstNonBlank(rs.getString("contrat_code"), rs.getString("legacy_contrat_code")));
    fields.put("contratId", rs.getObject("fk_contrat"));
    fields.put("contratCode", rs.getString("contrat_code"));
    fields.put("contratDesignation", rs.getString("contrat_designation"));
    item.put("fields", fields);
    item.put("contratId", rs.getObject("fk_contrat"));
    item.put("contratCode", rs.getString("contrat_code"));
    item.put("contratDesignation", rs.getString("contrat_designation"));
    return item;
  }

  private Map<String, Object> mapAyant(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> item = baseItem(rs, rs.getString("matricule"), rs.getString("nom_complet"));
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("MATRICULE", rs.getString("matricule"));
    fields.put("NOM", rs.getString("nom_complet"));
    fields.put("SEXE", rs.getString("sexe"));
    Date dn = rs.getDate("date_naissance");
    if (dn != null) {
      fields.put("DATENAIS", dn.toLocalDate().toString());
    }
    fields.put("RELATION", rs.getString("relation"));
    fields.put("CODEFAM", rs.getString("code_famille"));
    fields.put("CODESOC", rs.getString("legacy_codesoc"));
    fields.put("CONTRAT", firstNonBlank(rs.getString("contrat_code"), rs.getString("legacy_contrat_code")));
    fields.put("societeId", rs.getObject("fk_societe"));
    fields.put("societeDesignation", rs.getString("societe_designation"));
    fields.put("contratId", rs.getObject("fk_contrat"));
    fields.put("contratCode", rs.getString("contrat_code"));
    fields.put("contratDesignation", rs.getString("contrat_designation"));
    fields.put("medilineIpp", rs.getString("mediline_ipp"));
    item.put("fields", fields);
    item.put("societeId", rs.getObject("fk_societe"));
    item.put("societeDesignation", rs.getString("societe_designation"));
    item.put("contratId", rs.getObject("fk_contrat"));
    item.put("contratCode", rs.getString("contrat_code"));
    item.put("contratDesignation", rs.getString("contrat_designation"));
    item.put("sexe", rs.getString("sexe"));
    item.put("medilineIpp", rs.getString("mediline_ipp"));
    return item;
  }

  private Map<String, Object> baseItem(java.sql.ResultSet rs, String key, String label) throws java.sql.SQLException {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", rs.getLong("id"));
    item.put("key", trim(key));
    item.put("label", trim(label));
    item.put("actif", rs.getBoolean("actif"));
    item.put("sourceSystem", rs.getString("source_system"));
    item.put("legacyKey", rs.getString("legacy_code"));
    item.put("syncStatus", rs.getString("sync_status"));
    item.put("version", rs.getInt("version"));
    Timestamp ts = rs.getTimestamp("last_synced_at");
    item.put("lastSyncedAt", ts == null ? null : ts.toLocalDateTime());
    return item;
  }

  private static void appendTextFilter(StringBuilder sql, List<Object> args, String q, String... cols) {
    if (q == null || q.isBlank() || cols.length == 0) {
      return;
    }
    sql.append(" AND (");
    for (int i = 0; i < cols.length; i++) {
      if (i > 0) {
        sql.append(" OR ");
      }
      sql.append(cols[i]).append(" LIKE ?");
      args.add("%" + q.trim() + "%");
    }
    sql.append(")");
  }

  private static void appendActif(StringBuilder sql, List<Object> args, Boolean actif, String col) {
    if (actif != null) {
      sql.append(" AND ").append(col).append(" = ?");
      args.add(actif ? 1 : 0);
    }
  }

  private static int clampLimit(int limit) {
    return Math.min(Math.max(limit, 1), 500);
  }

  private String trim(String v) {
    return v == null ? null : v.trim();
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a.trim();
    }
    if (b != null && !b.isBlank()) {
      return b.trim();
    }
    return null;
  }
}
