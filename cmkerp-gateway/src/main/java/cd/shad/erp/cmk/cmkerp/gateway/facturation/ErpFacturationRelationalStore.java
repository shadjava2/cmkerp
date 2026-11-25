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
import java.util.Set;
import java.util.UUID;

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
    upsertAyantFromImport(matricule, nom, sexe, dateNais, nationalite, relation, codeFam, codeSoc, contrat,
        assurance, covid, location, source, fingerprint, null, null);
  }

  public void upsertAyantFromImport(String matricule, String nom, String sexe, LocalDate dateNais,
      String nationalite, String relation, String codeFam, String codeSoc, String contrat,
      String assurance, String covid, String location, String source, String fingerprint,
      String medilineIpp, Long medilinePatientId) {
    String m = trim(matricule);
    if (m == null || m.isBlank()) {
      return;
    }
    Long societeId = findSocieteId(codeSoc);
    Long contratId = findContratId(contrat);
    Long locationId = findLocationId(location);
    String linkStatus = (medilineIpp != null && !medilineIpp.isBlank()) ? "LINKED" : null;
    int updated = jdbc.update(
        """
            UPDATE erp_ayants_droit
            SET nom_complet = ?, sexe = ?, date_naissance = ?, nationalite = ?, relation = ?,
                code_famille = ?, legacy_codesoc = ?, fk_societe = COALESCE(?, fk_societe),
                legacy_contrat_code = ?, fk_contrat = COALESCE(?, fk_contrat),
                assurance = ?, covid = ?, legacy_location_code = ?, fk_location = COALESCE(?, fk_location),
                source_legacy = ?,
                mediline_ipp = COALESCE(?, mediline_ipp),
                mediline_patient_id = COALESCE(?, mediline_patient_id),
                patient_link_status = CASE
                  WHEN ? IS NOT NULL THEN ?
                  ELSE patient_link_status END,
                sync_status = 'IMPORTED', sync_fingerprint = ?,
                last_synced_at = NOW(), source_system = 'IMPORT_CLINIQUE', dateupdate = NOW()
            WHERE matricule = ?
            """,
        nom, sexe, dateNais == null ? null : Date.valueOf(dateNais), nationalite, relation, codeFam,
        trim(codeSoc), societeId, trim(contrat), contratId, assurance, covid, trim(location), locationId,
        source, trim(medilineIpp), medilinePatientId, linkStatus, linkStatus == null ? "UNLINKED" : linkStatus,
        fingerprint, m);
    if (updated == 0) {
      jdbc.update(
          """
              INSERT INTO erp_ayants_droit
                (matricule, nom_complet, sexe, date_naissance, nationalite, relation, code_famille,
                 fk_societe, legacy_codesoc, fk_contrat, legacy_contrat_code, assurance, covid,
                 legacy_location_code, fk_location, source_legacy, mediline_ipp, mediline_patient_id,
                 patient_link_status, actif, source_system, legacy_code,
                 sync_status, sync_fingerprint, last_synced_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', ?, NOW())
              """,
          m, nom, sexe, dateNais == null ? null : Date.valueOf(dateNais), nationalite, relation, codeFam,
          societeId, trim(codeSoc), contratId, trim(contrat), assurance, covid, trim(location), locationId,
          source, trim(medilineIpp), medilinePatientId,
          linkStatus == null ? "UNLINKED" : linkStatus, m, fingerprint);
    }
  }

  public int linkAyantPatient(long id, String medilineIpp, Long medilinePatientId, String status, int version, Long userId) {
    String safeStatus = status == null || status.isBlank() ? "LINKED" : status.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("LINKED", "UNLINKED", "REVIEW").contains(safeStatus)) {
      safeStatus = "LINKED";
    }
    return jdbc.update(
        """
            UPDATE erp_ayants_droit
            SET mediline_ipp = ?, mediline_patient_id = ?, patient_link_status = ?,
                version = version + 1, userupdateid = ?, dateupdate = NOW()
            WHERE id = ? AND version = ?
            """,
        trim(medilineIpp), medilinePatientId, safeStatus, userId, id, version);
  }

  public int updateAyantById(long id, String nom, String sexe, LocalDate dateNais, String relation,
      String codeFam, String codeSoc, String contrat, String medilineIpp, Long medilinePatientId,
      String patientLinkStatus, int version, Long userId) {
    Long societeId = findSocieteId(codeSoc);
    Long contratId = findContratId(contrat);
    String link = patientLinkStatus;
    if ((link == null || link.isBlank()) && medilineIpp != null && !medilineIpp.isBlank()) {
      link = "LINKED";
    }
    if (link == null || link.isBlank()) {
      link = "UNLINKED";
    }
    return jdbc.update(
        """
            UPDATE erp_ayants_droit
            SET nom_complet = ?, sexe = ?, date_naissance = ?, relation = ?, code_famille = ?,
                legacy_codesoc = ?, fk_societe = ?, legacy_contrat_code = ?, fk_contrat = ?,
                mediline_ipp = ?, mediline_patient_id = ?, patient_link_status = ?,
                version = version + 1, userupdateid = ?, dateupdate = NOW(), sync_status = 'LOCAL_ONLY'
            WHERE id = ? AND version = ?
            """,
        nom, sexe, dateNais == null ? null : Date.valueOf(dateNais), relation, codeFam,
        trim(codeSoc), societeId, trim(contrat), contratId,
        trim(medilineIpp), medilinePatientId, link.toUpperCase(Locale.ROOT),
        userId, id, version);
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
                   a.mediline_ipp, a.mediline_patient_id, a.patient_link_status,
                   a.actif, a.source_system, a.legacy_code, a.sync_status, a.version, a.last_synced_at
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
                   a.mediline_ipp, a.mediline_patient_id, a.patient_link_status,
                   a.actif, a.source_system, a.legacy_code, a.sync_status, a.version, a.last_synced_at
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

  public Long findServiceId(String code) {
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

  public Long findImputationId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_imputations WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  public Long findLocationId(String code) {
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
    fields.put("medilinePatientId", rs.getObject("mediline_patient_id"));
    fields.put("patientLinkStatus", rs.getString("patient_link_status"));
    item.put("fields", fields);
    item.put("societeId", rs.getObject("fk_societe"));
    item.put("societeDesignation", rs.getString("societe_designation"));
    item.put("contratId", rs.getObject("fk_contrat"));
    item.put("contratCode", rs.getString("contrat_code"));
    item.put("contratDesignation", rs.getString("contrat_designation"));
    item.put("sexe", rs.getString("sexe"));
    item.put("medilineIpp", rs.getString("mediline_ipp"));
    item.put("medilinePatientId", rs.getObject("mediline_patient_id"));
    item.put("patientLinkStatus", rs.getString("patient_link_status"));
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

  // ---------- Mappings utilisateurs / pharmacies ----------

  public List<Map<String, Object>> listUserMappings(String q, String status, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT id, erp_user_id, source_system, legacy_user_code, legacy_location_code,
                   legacy_display_name, mapping_status, actif, datecreate, dateupdate
            FROM erp_user_external_mappings WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (legacy_user_code LIKE ? OR legacy_display_name LIKE ? OR CAST(erp_user_id AS CHAR) LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" AND mapping_status = ?");
      args.add(status.trim().toUpperCase(Locale.ROOT));
    }
    sql.append(" ORDER BY legacy_user_code ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("erpUserId", rs.getObject("erp_user_id"));
      row.put("sourceSystem", rs.getString("source_system"));
      row.put("legacyUserCode", rs.getString("legacy_user_code"));
      row.put("legacyLocationCode", rs.getString("legacy_location_code"));
      row.put("legacyDisplayName", rs.getString("legacy_display_name"));
      row.put("mappingStatus", rs.getString("mapping_status"));
      row.put("actif", rs.getBoolean("actif"));
      return row;
    }, args.toArray());
  }

  public long countUserMappings(String q, String status) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM erp_user_external_mappings WHERE 1=1");
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (legacy_user_code LIKE ? OR legacy_display_name LIKE ? OR CAST(erp_user_id AS CHAR) LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" AND mapping_status = ?");
      args.add(status.trim().toUpperCase(Locale.ROOT));
    }
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  public int bindUserMapping(long id, Long erpUserId, boolean actif) {
    return jdbc.update(
        """
            UPDATE erp_user_external_mappings
            SET erp_user_id = ?, mapping_status = CASE WHEN ? IS NULL THEN 'UNMATCHED' ELSE 'MATCHED' END,
                actif = ?, dateupdate = NOW()
            WHERE id = ?
            """,
        erpUserId, erpUserId, actif ? 1 : 0, id);
  }

  public Optional<String> findLegacyUserCodeForErpUser(long erpUserId) {
    var list = jdbc.query(
        """
            SELECT legacy_user_code FROM erp_user_external_mappings
            WHERE erp_user_id = ? AND actif = 1 AND mapping_status = 'MATCHED'
            ORDER BY id DESC LIMIT 1
            """,
        (rs, i) -> rs.getString(1),
        erpUserId);
    return list.stream().findFirst().map(this::trim);
  }

  public List<Map<String, Object>> listPharmacyMappings(String q, String status, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT m.id, m.erp_pharmacy_id, m.source_system, m.legacy_pharmacy_code, m.legacy_designation,
                   m.mapping_status, m.actif, p.designation AS erp_designation
            FROM erp_pharmacy_external_mappings m
            LEFT JOIN pharmacies p ON p.id = m.erp_pharmacy_id
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (m.legacy_pharmacy_code LIKE ? OR m.legacy_designation LIKE ? OR p.designation LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" AND m.mapping_status = ?");
      args.add(status.trim().toUpperCase(Locale.ROOT));
    }
    sql.append(" ORDER BY m.legacy_pharmacy_code ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("erpPharmacyId", rs.getObject("erp_pharmacy_id"));
      row.put("erpDesignation", rs.getString("erp_designation"));
      row.put("legacyPharmacyCode", rs.getString("legacy_pharmacy_code"));
      row.put("legacyDesignation", rs.getString("legacy_designation"));
      row.put("mappingStatus", rs.getString("mapping_status"));
      row.put("actif", rs.getBoolean("actif"));
      return row;
    }, args.toArray());
  }

  public long countPharmacyMappings(String q, String status) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT COUNT(*) FROM erp_pharmacy_external_mappings m
            LEFT JOIN pharmacies p ON p.id = m.erp_pharmacy_id WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (m.legacy_pharmacy_code LIKE ? OR m.legacy_designation LIKE ? OR p.designation LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" AND m.mapping_status = ?");
      args.add(status.trim().toUpperCase(Locale.ROOT));
    }
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  public int bindPharmacyMapping(long id, Long erpPharmacyId, boolean actif) {
    return jdbc.update(
        """
            UPDATE erp_pharmacy_external_mappings
            SET erp_pharmacy_id = ?, mapping_status = CASE WHEN ? IS NULL THEN 'UNMATCHED' ELSE 'MATCHED' END,
                actif = ?, dateupdate = NOW()
            WHERE id = ?
            """,
        erpPharmacyId, erpPharmacyId, actif ? 1 : 0, id);
  }

  public List<Map<String, Object>> listErpPharmacies(String q, int limit) {
    StringBuilder sql = new StringBuilder(
        "SELECT id, designation, codeimmo, typepharmacie, fkSite FROM pharmacies WHERE 1=1");
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (designation LIKE ? OR codeimmo LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
    }
    sql.append(" ORDER BY designation ASC LIMIT ?");
    args.add(Math.min(Math.max(limit, 1), 200));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("designation", rs.getString("designation"));
      row.put("code", rs.getString("codeimmo"));
      row.put("type", rs.getString("typepharmacie"));
      row.put("siteId", rs.getObject("fkSite"));
      return row;
    }, args.toArray());
  }

  // ---------- Transactions facturation ----------

  /**
   * Enregistre une prestation ERP destinée à CLINIQUE.
   * {@code syncStatus} = PENDING si écriture Beach autorisée pour TPRESTATION, sinon SUSPENDED.
   * Ne jamais utiliser LOCAL_ONLY pour une intention de réplication (leçon TES/contrats).
   */
  public Map<String, Object> insertPrestation(Map<String, Object> body, Long userId, String syncStatus) {
    KeyHolder keys = new GeneratedKeyHolder();
    String operationId = "PREST-" + UUID.randomUUID();
    String status = normalizeClinicBoundStatus(syncStatus);
    jdbc.update(con -> {
      var ps = con.prepareStatement(
          """
              INSERT INTO erp_facturation_prestations
                (operation_id, fk_ayant_droit, legacy_matricule, mediline_patient_id, fk_societe, fk_contrat,
                 fk_tarif, legacy_tarif_code, fk_service, fk_classe, fk_imputation, fk_medecin,
                 legacy_medecin_code, fk_location, montant, date_prestation, payload_snapshot_json,
                 actif, source_system, sync_status, usercreateid, userupdateid)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'OPERATION_ERP', ?, ?, ?)
              """,
          new String[] {"id"});
      int i = 1;
      ps.setString(i++, operationId);
      setLong(ps, i++, longOrNull(body.get("ayantDroitId")));
      ps.setString(i++, str(body.get("matricule")));
      setLong(ps, i++, longOrNull(body.get("medilinePatientId")));
      setLong(ps, i++, longOrNull(body.get("societeId")));
      setLong(ps, i++, longOrNull(body.get("contratId")));
      setLong(ps, i++, longOrNull(body.get("tarifId")));
      ps.setString(i++, str(body.get("tarifCode")));
      setLong(ps, i++, longOrNull(body.get("serviceId")));
      setLong(ps, i++, longOrNull(body.get("classeId")));
      setLong(ps, i++, longOrNull(body.get("imputationId")));
      setLong(ps, i++, longOrNull(body.get("medecinId")));
      ps.setString(i++, str(body.get("medecinCode")));
      setLong(ps, i++, longOrNull(body.get("locationId")));
      ps.setBigDecimal(i++, decimalOrZero(body.get("montant")));
      Object dateRaw = body.get("datePrestation");
      if (dateRaw == null || String.valueOf(dateRaw).isBlank()) {
        ps.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      } else {
        String text = String.valueOf(dateRaw).trim().replace('T', ' ');
        if (text.length() == 10) {
          text = text + " 00:00:00";
        } else if (text.length() > 19) {
          text = text.substring(0, 19);
        }
        ps.setTimestamp(i++, Timestamp.valueOf(text));
      }
      ps.setString(i++, str(body.get("snapshotJson")));
      ps.setString(i++, status);
      setLong(ps, i++, userId);
      setLong(ps, i++, userId);
      return ps;
    }, keys);
    Number key = keys.getKey();
    long id = key == null ? 0L : key.longValue();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("operationId", operationId);
    result.put("syncStatus", status);
    return result;
  }

  public Map<String, Object> insertMedicament(Map<String, Object> body, Long userId, String syncStatus) {
    KeyHolder keys = new GeneratedKeyHolder();
    String operationId = "MED-" + UUID.randomUUID();
    String status = normalizeClinicBoundStatus(syncStatus);
    jdbc.update(con -> {
      var ps = con.prepareStatement(
          """
              INSERT INTO erp_facturation_medicaments
                (operation_id, fk_ayant_droit, legacy_matricule, mediline_patient_id, fk_societe, fk_contrat,
                 fk_product, legacy_product_code, fk_medecin, legacy_medecin_code, fk_pharmacy,
                 legacy_pharmacy_code, fk_service, fk_location, quantite, montant, date_mouvement,
                 payload_snapshot_json, actif, source_system, sync_status, usercreateid, userupdateid)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'OPERATION_ERP', ?, ?, ?)
              """,
          new String[] {"id"});
      int i = 1;
      ps.setString(i++, operationId);
      setLong(ps, i++, longOrNull(body.get("ayantDroitId")));
      ps.setString(i++, str(body.get("matricule")));
      setLong(ps, i++, longOrNull(body.get("medilinePatientId")));
      setLong(ps, i++, longOrNull(body.get("societeId")));
      setLong(ps, i++, longOrNull(body.get("contratId")));
      setLong(ps, i++, longOrNull(body.get("productId")));
      ps.setString(i++, str(body.get("productCode")));
      setLong(ps, i++, longOrNull(body.get("medecinId")));
      ps.setString(i++, str(body.get("medecinCode")));
      setLong(ps, i++, longOrNull(body.get("pharmacyId")));
      ps.setString(i++, str(body.get("pharmacyCode")));
      setLong(ps, i++, longOrNull(body.get("serviceId")));
      setLong(ps, i++, longOrNull(body.get("locationId")));
      ps.setBigDecimal(i++, decimalOrZero(body.get("quantite")));
      ps.setBigDecimal(i++, decimalOrZero(body.get("montant")));
      Object dateRaw = body.get("dateMouvement");
      if (dateRaw == null || String.valueOf(dateRaw).isBlank()) {
        ps.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      } else {
        String text = String.valueOf(dateRaw).trim().replace('T', ' ');
        if (text.length() == 10) {
          text = text + " 00:00:00";
        } else if (text.length() > 19) {
          text = text.substring(0, 19);
        }
        ps.setTimestamp(i++, Timestamp.valueOf(text));
      }
      ps.setString(i++, str(body.get("snapshotJson")));
      ps.setString(i++, status);
      setLong(ps, i++, userId);
      setLong(ps, i++, userId);
      return ps;
    }, keys);
    Number key = keys.getKey();
    long id = key == null ? 0L : key.longValue();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("operationId", operationId);
    result.put("syncStatus", status);
    return result;
  }

  /**
   * Intention outbox pour une transaction facturation destinée à CLINIQUE Beach.
   * Aucune écriture SQL Server ici. Le worker ne traite que les entités whitelistées.
   */
  public void enqueueBillingOutbox(
      String operationId,
      String entityType,
      long entityId,
      String legacyKey,
      String operationType,
      String payloadJson,
      String correlationId,
      String status) {
    jdbc.update(
        """
            INSERT INTO erp_sync_outbox
              (operation_id, entity_type, entity_id, legacy_key, source_system, target_system,
               operation_type, payload_json, status, attempt_count, correlation_id)
            VALUES (?, ?, ?, ?, 'ERP', 'CLINIQUE_BEACH', ?, ?, ?, 0, ?)
            """,
        operationId,
        entityType,
        entityId,
        legacyKey,
        operationType,
        payloadJson,
        normalizeClinicBoundStatus(status),
        correlationId);
  }

  public boolean hasOpenBillingOutbox(long entityId, String entityType) {
    Long n = jdbc.queryForObject(
        """
            SELECT COUNT(*) FROM erp_sync_outbox
            WHERE entity_id = ? AND entity_type = ?
              AND status IN ('PENDING', 'SUSPENDED', 'IN_PROGRESS', 'UNCERTAIN')
            """,
        Long.class,
        entityId,
        entityType);
    return n != null && n > 0;
  }

  private static String normalizeClinicBoundStatus(String syncStatus) {
    if (syncStatus == null || syncStatus.isBlank()) {
      return "SUSPENDED";
    }
    String s = syncStatus.trim().toUpperCase(Locale.ROOT);
    if ("LOCAL_ONLY".equals(s)) {
      // Intention destinée à CLINIQUE : ne jamais perdre l'intention derrière LOCAL_ONLY.
      return "SUSPENDED";
    }
    if ("PENDING".equals(s) || "SUSPENDED".equals(s) || "BLOCKED_RULES".equals(s) || "BLOCKED_MAPPING".equals(s)) {
      return s;
    }
    return "SUSPENDED";
  }

  public List<Map<String, Object>> listPrestations(String q, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT p.id, p.operation_id, p.legacy_matricule, p.montant, p.date_prestation, p.sync_status,
                   a.nom_complet AS patient_nom, s.designation AS societe_designation,
                   c.designation AS contrat_designation, t.designation AS tarif_designation,
                   m.nom AS medecin_nom, sv.libelle AS service_libelle
            FROM erp_facturation_prestations p
            LEFT JOIN erp_ayants_droit a ON a.id = p.fk_ayant_droit
            LEFT JOIN erp_societes s ON s.id = p.fk_societe
            LEFT JOIN erp_contrats c ON c.id = p.fk_contrat
            LEFT JOIN erp_tarifs t ON t.id = p.fk_tarif
            LEFT JOIN erp_medecins m ON m.id = p.fk_medecin
            LEFT JOIN erp_services sv ON sv.id = p.fk_service
            WHERE p.actif = 1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (p.legacy_matricule LIKE ? OR a.nom_complet LIKE ? OR t.designation LIKE ? OR s.designation LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    sql.append(" ORDER BY p.id DESC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("operationId", rs.getString("operation_id"));
      row.put("matricule", rs.getString("legacy_matricule"));
      row.put("patientNom", rs.getString("patient_nom"));
      row.put("societeDesignation", rs.getString("societe_designation"));
      row.put("contratDesignation", rs.getString("contrat_designation"));
      row.put("tarifDesignation", rs.getString("tarif_designation"));
      row.put("medecinNom", rs.getString("medecin_nom"));
      row.put("serviceLibelle", rs.getString("service_libelle"));
      row.put("montant", rs.getBigDecimal("montant"));
      row.put("datePrestation", rs.getTimestamp("date_prestation"));
      row.put("syncStatus", rs.getString("sync_status"));
      return row;
    }, args.toArray());
  }

  public long countPrestations(String q) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT COUNT(*) FROM erp_facturation_prestations p
            LEFT JOIN erp_ayants_droit a ON a.id = p.fk_ayant_droit
            LEFT JOIN erp_tarifs t ON t.id = p.fk_tarif
            LEFT JOIN erp_societes s ON s.id = p.fk_societe
            WHERE p.actif = 1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (p.legacy_matricule LIKE ? OR a.nom_complet LIKE ? OR t.designation LIKE ? OR s.designation LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  public List<Map<String, Object>> listMedicamentsTx(String q, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT m.id, m.operation_id, m.legacy_matricule, m.quantite, m.montant, m.date_mouvement, m.sync_status,
                   a.nom_complet AS patient_nom, s.designation AS societe_designation,
                   pr.nomcommercial AS produit_designation, m.legacy_product_code,
                   med.nom AS medecin_nom, ph.designation AS pharmacie_designation
            FROM erp_facturation_medicaments m
            LEFT JOIN erp_ayants_droit a ON a.id = m.fk_ayant_droit
            LEFT JOIN erp_societes s ON s.id = m.fk_societe
            LEFT JOIN produits pr ON pr.id = m.fk_product
            LEFT JOIN erp_medecins med ON med.id = m.fk_medecin
            LEFT JOIN pharmacies ph ON ph.id = m.fk_pharmacy
            WHERE m.actif = 1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (m.legacy_matricule LIKE ? OR a.nom_complet LIKE ? OR pr.nomcommercial LIKE ? OR m.legacy_product_code LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    sql.append(" ORDER BY m.id DESC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("operationId", rs.getString("operation_id"));
      row.put("matricule", rs.getString("legacy_matricule"));
      row.put("patientNom", rs.getString("patient_nom"));
      row.put("societeDesignation", rs.getString("societe_designation"));
      row.put("produitDesignation", firstNonBlank(rs.getString("produit_designation"), rs.getString("legacy_product_code")));
      row.put("medecinNom", rs.getString("medecin_nom"));
      row.put("pharmacieDesignation", rs.getString("pharmacie_designation"));
      row.put("quantite", rs.getBigDecimal("quantite"));
      row.put("montant", rs.getBigDecimal("montant"));
      row.put("dateMouvement", rs.getTimestamp("date_mouvement"));
      row.put("syncStatus", rs.getString("sync_status"));
      return row;
    }, args.toArray());
  }

  public long countMedicamentsTx(String q) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT COUNT(*) FROM erp_facturation_medicaments m
            LEFT JOIN erp_ayants_droit a ON a.id = m.fk_ayant_droit
            LEFT JOIN produits pr ON pr.id = m.fk_product
            WHERE m.actif = 1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (m.legacy_matricule LIKE ? OR a.nom_complet LIKE ? OR pr.nomcommercial LIKE ? OR m.legacy_product_code LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  public List<Map<String, Object>> searchProducts(String q, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT p.id, p.codebarre, p.CODECLINIQUE, p.nomcommercial, p.nomscientifique,
                   m.legacy_product_code, m.mapping_status
            FROM produits p
            LEFT JOIN erp_product_external_mappings m ON m.erp_product_id = p.id
            WHERE 1=1
            """);
    var args = new ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append("""
           AND (p.nomcommercial LIKE ? OR p.nomscientifique LIKE ? OR p.codebarre LIKE ?
                OR p.CODECLINIQUE LIKE ? OR m.legacy_product_code LIKE ?)
          """);
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    sql.append(" ORDER BY p.nomcommercial ASC LIMIT ? OFFSET ?");
    args.add(clampLimit(limit));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("code", firstNonBlank(rs.getString("CODECLINIQUE"), rs.getString("codebarre")));
      row.put("designation", rs.getString("nomcommercial"));
      row.put("dci", rs.getString("nomscientifique"));
      row.put("legacyCode", rs.getString("legacy_product_code"));
      row.put("mappingStatus", rs.getString("mapping_status"));
      return row;
    }, args.toArray());
  }

  public Map<String, Object> dashboardSnapshot(String from, String to) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ayantsDroit", scalar("SELECT COUNT(*) FROM erp_ayants_droit WHERE actif = 1"));
    out.put("societes", scalar("SELECT COUNT(*) FROM erp_societes WHERE actif = 1"));
    out.put("contrats", scalar("SELECT COUNT(*) FROM erp_contrats WHERE actif = 1"));
    out.put("tarifs", scalar("SELECT COUNT(*) FROM erp_tarifs WHERE actif = 1"));
    out.put("patientsLinked", scalar(
        "SELECT COUNT(*) FROM erp_ayants_droit WHERE patient_link_status = 'LINKED'"));
    out.put("patientsUnlinked", scalar(
        "SELECT COUNT(*) FROM erp_ayants_droit WHERE patient_link_status = 'UNLINKED' OR patient_link_status IS NULL"));
    String dateFilterPrest = "";
    String dateFilterMed = "";
    var argsP = new ArrayList<Object>();
    var argsM = new ArrayList<Object>();
    if (from != null && !from.isBlank()) {
      dateFilterPrest += " AND DATE(date_prestation) >= ?";
      dateFilterMed += " AND DATE(date_mouvement) >= ?";
      argsP.add(from);
      argsM.add(from);
    }
    if (to != null && !to.isBlank()) {
      dateFilterPrest += " AND DATE(date_prestation) <= ?";
      dateFilterMed += " AND DATE(date_mouvement) <= ?";
      argsP.add(to);
      argsM.add(to);
    }
    out.put("prestationsCount", scalar(
        "SELECT COUNT(*) FROM erp_facturation_prestations WHERE actif = 1" + dateFilterPrest, argsP.toArray()));
    out.put("prestationsMontant", scalarDecimal(
        "SELECT COALESCE(SUM(montant),0) FROM erp_facturation_prestations WHERE actif = 1" + dateFilterPrest,
        argsP.toArray()));
    out.put("medicamentsCount", scalar(
        "SELECT COUNT(*) FROM erp_facturation_medicaments WHERE actif = 1" + dateFilterMed, argsM.toArray()));
    out.put("medicamentsMontant", scalarDecimal(
        "SELECT COALESCE(SUM(montant),0) FROM erp_facturation_medicaments WHERE actif = 1" + dateFilterMed,
        argsM.toArray()));
    out.put("bySocietePrestations", jdbc.query(
        """
            SELECT s.designation AS label, COUNT(*) AS cnt, COALESCE(SUM(p.montant),0) AS montant
            FROM erp_facturation_prestations p
            LEFT JOIN erp_societes s ON s.id = p.fk_societe
            WHERE p.actif = 1
            GROUP BY s.designation
            ORDER BY montant DESC
            LIMIT 10
            """,
        (rs, i) -> Map.of(
            "label", rs.getString("label") == null ? "Non rapproché" : rs.getString("label"),
            "count", rs.getLong("cnt"),
            "montant", rs.getBigDecimal("montant"))));
    out.put("byServicePrestations", jdbc.query(
        """
            SELECT sv.libelle AS label, COUNT(*) AS cnt, COALESCE(SUM(p.montant),0) AS montant
            FROM erp_facturation_prestations p
            LEFT JOIN erp_services sv ON sv.id = p.fk_service
            WHERE p.actif = 1
            GROUP BY sv.libelle
            ORDER BY montant DESC
            LIMIT 10
            """,
        (rs, i) -> Map.of(
            "label", rs.getString("label") == null ? "Non rapproché" : rs.getString("label"),
            "count", rs.getLong("cnt"),
            "montant", rs.getBigDecimal("montant"))));
    out.put("patientsReview", scalar(
        "SELECT COUNT(*) FROM erp_ayants_droit WHERE patient_link_status IN ('REVIEW','AMBIGUOUS')"));
    out.put("patientsConflict", scalar(
        "SELECT COUNT(*) FROM erp_ayants_droit WHERE patient_link_status = 'CONFLICT'"));
    out.put("prestationsImported", scalar(
        "SELECT COUNT(*) FROM erp_facturation_prestations WHERE actif = 1 AND source_system = 'CLINIQUE_BEACH'"
            + dateFilterPrest, argsP.toArray()));
    out.put("prestationsErpLocal", scalar(
        "SELECT COUNT(*) FROM erp_facturation_prestations WHERE actif = 1 AND source_system = 'OPERATION_ERP'"
            + dateFilterPrest, argsP.toArray()));
    out.put("medicamentsImported", scalar(
        "SELECT COUNT(*) FROM erp_facturation_medicaments WHERE actif = 1 AND source_system = 'CLINIQUE_BEACH'"
            + dateFilterMed, argsM.toArray()));
    out.put("medicamentsErpLocal", scalar(
        "SELECT COUNT(*) FROM erp_facturation_medicaments WHERE actif = 1 AND source_system = 'OPERATION_ERP'"
            + dateFilterMed, argsM.toArray()));
    out.put("stockCompartments", scalar("SELECT COUNT(*) FROM erp_stock_clinique"));
    out.put("stockMappedCompartments", scalar(
        "SELECT COUNT(*) FROM erp_stock_compartment_mappings WHERE mapping_status = 'MAPPED' AND actif = 1"));
    out.put("coveragePrestations", jdbc.queryForMap(
        """
            SELECT MIN(DATE(date_prestation)) AS minDate, MAX(DATE(date_prestation)) AS maxDate
            FROM erp_facturation_prestations WHERE actif = 1 AND date_prestation IS NOT NULL
            """));
    out.put("coverageMedicaments", jdbc.queryForMap(
        """
            SELECT MIN(DATE(date_mouvement)) AS minDate, MAX(DATE(date_mouvement)) AS maxDate
            FROM erp_facturation_medicaments WHERE actif = 1 AND date_mouvement IS NOT NULL
            """));
    out.put("byContratPrestations", jdbc.query(
        """
            SELECT COALESCE(c.designation, c.code, p.fk_contrat) AS label,
                   COUNT(*) AS cnt, COALESCE(SUM(p.montant),0) AS montant
            FROM erp_facturation_prestations p
            LEFT JOIN erp_contrats c ON c.id = p.fk_contrat
            WHERE p.actif = 1
            GROUP BY COALESCE(c.designation, c.code, p.fk_contrat)
            ORDER BY montant DESC
            LIMIT 10
            """,
        (rs, i) -> Map.of(
            "label", rs.getString("label") == null ? "Non rapproché" : String.valueOf(rs.getString("label")),
            "count", rs.getLong("cnt"),
            "montant", rs.getBigDecimal("montant"))));
    out.put("byMedecinPrestations", jdbc.query(
        """
            SELECT COALESCE(m.nom, p.legacy_medecin_code) AS label,
                   COUNT(*) AS cnt, COALESCE(SUM(p.montant),0) AS montant
            FROM erp_facturation_prestations p
            LEFT JOIN erp_medecins m ON m.id = p.fk_medecin
            WHERE p.actif = 1
            GROUP BY COALESCE(m.nom, p.legacy_medecin_code)
            ORDER BY montant DESC
            LIMIT 10
            """,
        (rs, i) -> Map.of(
            "label", rs.getString("label") == null ? "Non rapproché" : rs.getString("label"),
            "count", rs.getLong("cnt"),
            "montant", rs.getBigDecimal("montant"))));
    out.put("byPharmacyMedicaments", jdbc.query(
        """
            SELECT COALESCE(ph.designation, m.legacy_pharmacy_code) AS label,
                   COUNT(*) AS cnt, COALESCE(SUM(m.montant),0) AS montant
            FROM erp_facturation_medicaments m
            LEFT JOIN pharmacies ph ON ph.id = m.fk_pharmacy
            WHERE m.actif = 1
            GROUP BY COALESCE(ph.designation, m.legacy_pharmacy_code)
            ORDER BY montant DESC
            LIMIT 10
            """,
        (rs, i) -> Map.of(
            "label", rs.getString("label") == null ? "Non rapproché" : rs.getString("label"),
            "count", rs.getLong("cnt"),
            "montant", rs.getBigDecimal("montant"))));
    out.put("monthlyEvolution", jdbc.query(
        """
            SELECT periode, SUM(prest_montant) AS prestationsMontant, SUM(med_montant) AS medicamentsMontant,
                   SUM(prest_cnt) AS prestationsCount, SUM(med_cnt) AS medicamentsCount
            FROM (
              SELECT DATE_FORMAT(date_prestation, '%Y-%m') AS periode,
                     COALESCE(SUM(montant),0) AS prest_montant, COUNT(*) AS prest_cnt,
                     0 AS med_montant, 0 AS med_cnt
              FROM erp_facturation_prestations WHERE actif = 1 AND date_prestation IS NOT NULL
              GROUP BY DATE_FORMAT(date_prestation, '%Y-%m')
              UNION ALL
              SELECT DATE_FORMAT(date_mouvement, '%Y-%m') AS periode,
                     0, 0, COALESCE(SUM(montant),0), COUNT(*)
              FROM erp_facturation_medicaments WHERE actif = 1 AND date_mouvement IS NOT NULL
              GROUP BY DATE_FORMAT(date_mouvement, '%Y-%m')
            ) x
            GROUP BY periode
            ORDER BY periode DESC
            LIMIT 24
            """,
        (rs, i) -> {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("periode", rs.getString("periode"));
          row.put("prestationsMontant", rs.getBigDecimal("prestationsMontant"));
          row.put("medicamentsMontant", rs.getBigDecimal("medicamentsMontant"));
          row.put("prestationsCount", rs.getLong("prestationsCount"));
          row.put("medicamentsCount", rs.getLong("medicamentsCount"));
          return row;
        }));
    out.put("amountDisclaimer",
        "Montants = transactions enregistrées / importées. Ne pas interpréter comme encaissements.");
    out.put("sourceOfTruth", "erp_relational_tables");
    out.put("referentialsBridge", "erp_clinic_referentials (import/compat only)");
    return out;
  }

  public Long findMedecinId(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_medecins WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  public Long findAyantIdByMatricule(String matricule) {
    String c = trim(matricule);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query(
        "SELECT id FROM erp_ayants_droit WHERE matricule = ? ORDER BY id LIMIT 1",
        (rs, i) -> rs.getLong(1),
        c);
    return list.stream().findFirst().orElse(null);
  }

  public Long findProductIdByLegacyCode(String legacyCode) {
    String c = trim(legacyCode);
    if (c == null || c.isBlank()) {
      return null;
    }
    var mapped = jdbc.query(
        """
            SELECT erp_product_id FROM erp_product_external_mappings
            WHERE legacy_product_code = ? AND erp_product_id IS NOT NULL
            ORDER BY id LIMIT 1
            """,
        (rs, i) -> rs.getLong(1),
        c);
    if (!mapped.isEmpty()) {
      return mapped.get(0);
    }
    var byClinic = jdbc.query(
        "SELECT id FROM produits WHERE CODECLINIQUE = ? OR CODE = ? LIMIT 1",
        (rs, i) -> rs.getLong(1),
        c,
        c);
    return byClinic.stream().findFirst().orElse(null);
  }

  public Long findPharmacyIdByLegacyCode(String legacyCode) {
    String c = trim(legacyCode);
    if (c == null || c.isBlank()) {
      return null;
    }
    var mapped = jdbc.query(
        """
            SELECT erp_pharmacy_id FROM erp_pharmacy_external_mappings
            WHERE legacy_pharmacy_code = ? AND erp_pharmacy_id IS NOT NULL
            ORDER BY id LIMIT 1
            """,
        (rs, i) -> rs.getLong(1),
        c);
    return mapped.stream().findFirst().orElse(null);
  }

  public Long findTarifIdByCode(String code) {
    String c = trim(code);
    if (c == null || c.isBlank()) {
      return null;
    }
    var list = jdbc.query("SELECT id FROM erp_tarifs WHERE code = ?", (rs, i) -> rs.getLong(1), c);
    return list.stream().findFirst().orElse(null);
  }

  public long countPrestations() {
    Long n = jdbc.queryForObject("SELECT COUNT(*) FROM erp_facturation_prestations", Long.class);
    return n == null ? 0L : n;
  }

  public long countMedicaments() {
    Long n = jdbc.queryForObject("SELECT COUNT(*) FROM erp_facturation_medicaments", Long.class);
    return n == null ? 0L : n;
  }

  public boolean existsPrestationLegacy(String legacyId) {
    String c = trim(legacyId);
    if (c == null || c.isBlank()) {
      return false;
    }
    Long n = jdbc.queryForObject(
        "SELECT COUNT(*) FROM erp_facturation_prestations WHERE legacy_id = ?",
        Long.class,
        c);
    return n != null && n > 0;
  }

  public boolean existsMedicamentLegacy(String legacyId) {
    String c = trim(legacyId);
    if (c == null || c.isBlank()) {
      return false;
    }
    Long n = jdbc.queryForObject(
        "SELECT COUNT(*) FROM erp_facturation_medicaments WHERE legacy_id = ?",
        Long.class,
        c);
    return n != null && n > 0;
  }

  public int upsertPrestationHistory(
      String legacyId,
      String idmvt,
      Long ayantId,
      String matricule,
      Long societeId,
      Long contratId,
      Long tarifId,
      String tarifCode,
      Long serviceId,
      Long imputationId,
      Long medecinId,
      String medecinCode,
      Long locationId,
      BigDecimal montant,
      Timestamp datePrestation,
      String snapshotJson,
      String fingerprint) {
    return jdbc.update(
        """
            INSERT INTO erp_facturation_prestations
              (legacy_id, operation_id, legacy_idmvt, fk_ayant_droit, legacy_matricule, fk_societe, fk_contrat,
               fk_tarif, legacy_tarif_code, fk_service, fk_imputation, fk_medecin, legacy_medecin_code,
               fk_location, montant, date_prestation, payload_snapshot_json, actif, source_system,
               sync_status, sync_fingerprint, last_synced_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'CLINIQUE_BEACH', 'SYNCED', ?, NOW(), 1)
            ON DUPLICATE KEY UPDATE
              legacy_idmvt = VALUES(legacy_idmvt),
              fk_ayant_droit = COALESCE(VALUES(fk_ayant_droit), fk_ayant_droit),
              legacy_matricule = COALESCE(VALUES(legacy_matricule), legacy_matricule),
              fk_societe = COALESCE(VALUES(fk_societe), fk_societe),
              fk_contrat = COALESCE(VALUES(fk_contrat), fk_contrat),
              fk_tarif = COALESCE(VALUES(fk_tarif), fk_tarif),
              legacy_tarif_code = COALESCE(VALUES(legacy_tarif_code), legacy_tarif_code),
              fk_service = COALESCE(VALUES(fk_service), fk_service),
              fk_imputation = COALESCE(VALUES(fk_imputation), fk_imputation),
              fk_medecin = COALESCE(VALUES(fk_medecin), fk_medecin),
              legacy_medecin_code = COALESCE(VALUES(legacy_medecin_code), legacy_medecin_code),
              fk_location = COALESCE(VALUES(fk_location), fk_location),
              montant = VALUES(montant),
              date_prestation = VALUES(date_prestation),
              payload_snapshot_json = VALUES(payload_snapshot_json),
              sync_fingerprint = VALUES(sync_fingerprint),
              last_synced_at = NOW(),
              version = version + 1
            """,
        legacyId,
        "HIST-PREST-" + legacyId,
        idmvt,
        ayantId,
        matricule,
        societeId,
        contratId,
        tarifId,
        tarifCode,
        serviceId,
        imputationId,
        medecinId,
        medecinCode,
        locationId,
        montant == null ? BigDecimal.ZERO : montant,
        datePrestation,
        snapshotJson,
        fingerprint);
  }

  public int upsertMedicamentHistory(
      String legacyId,
      Long ayantId,
      String matricule,
      Long societeId,
      Long contratId,
      Long productId,
      String productCode,
      Long medecinId,
      String medecinCode,
      Long pharmacyId,
      String pharmacyCode,
      Long serviceId,
      Long locationId,
      BigDecimal quantite,
      BigDecimal montant,
      Timestamp dateMouvement,
      String snapshotJson,
      String fingerprint) {
    return jdbc.update(
        """
            INSERT INTO erp_facturation_medicaments
              (legacy_id, operation_id, fk_ayant_droit, legacy_matricule, fk_societe, fk_contrat,
               fk_product, legacy_product_code, fk_medecin, legacy_medecin_code, fk_pharmacy,
               legacy_pharmacy_code, fk_service, fk_location, quantite, montant, date_mouvement,
               payload_snapshot_json, actif, source_system, sync_status, sync_fingerprint,
               last_synced_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'CLINIQUE_BEACH', 'SYNCED', ?, NOW(), 1)
            ON DUPLICATE KEY UPDATE
              fk_ayant_droit = COALESCE(VALUES(fk_ayant_droit), fk_ayant_droit),
              legacy_matricule = COALESCE(VALUES(legacy_matricule), legacy_matricule),
              fk_societe = COALESCE(VALUES(fk_societe), fk_societe),
              fk_contrat = COALESCE(VALUES(fk_contrat), fk_contrat),
              fk_product = COALESCE(VALUES(fk_product), fk_product),
              legacy_product_code = COALESCE(VALUES(legacy_product_code), legacy_product_code),
              fk_medecin = COALESCE(VALUES(fk_medecin), fk_medecin),
              legacy_medecin_code = COALESCE(VALUES(legacy_medecin_code), legacy_medecin_code),
              fk_pharmacy = COALESCE(VALUES(fk_pharmacy), fk_pharmacy),
              legacy_pharmacy_code = COALESCE(VALUES(legacy_pharmacy_code), legacy_pharmacy_code),
              fk_service = COALESCE(VALUES(fk_service), fk_service),
              fk_location = COALESCE(VALUES(fk_location), fk_location),
              quantite = VALUES(quantite),
              montant = VALUES(montant),
              date_mouvement = VALUES(date_mouvement),
              payload_snapshot_json = VALUES(payload_snapshot_json),
              sync_fingerprint = VALUES(sync_fingerprint),
              last_synced_at = NOW(),
              version = version + 1
            """,
        legacyId,
        "HIST-MED-" + legacyId,
        ayantId,
        matricule,
        societeId,
        contratId,
        productId,
        productCode,
        medecinId,
        medecinCode,
        pharmacyId,
        pharmacyCode,
        serviceId,
        locationId,
        quantite == null ? BigDecimal.ZERO : quantite,
        montant == null ? BigDecimal.ZERO : montant,
        dateMouvement,
        snapshotJson,
        fingerprint);
  }

  public int upsertStockCompartment(
      String productCode, Long productId, String compartment, BigDecimal qty, String fingerprint) {
    return jdbc.update(
        """
            INSERT INTO erp_stock_clinique
              (legacy_product_code, fk_product, compartment_code, quantity, source_system,
               sync_status, sync_fingerprint, last_synced_at, version)
            VALUES (?, ?, ?, ?, 'CLINIQUE_BEACH', 'SYNCED', ?, NOW(), 1)
            ON DUPLICATE KEY UPDATE
              fk_product = COALESCE(VALUES(fk_product), fk_product),
              quantity = VALUES(quantity),
              sync_fingerprint = VALUES(sync_fingerprint),
              last_synced_at = NOW(),
              version = version + 1
            """,
        productCode,
        productId,
        compartment,
        qty == null ? BigDecimal.ZERO : qty,
        fingerprint);
  }

  public List<Map<String, Object>> listAyantsForMatch(String status, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT id, matricule, nom_complet, date_naissance, mediline_ipp, mediline_patient_id,
                   patient_link_status
            FROM erp_ayants_droit
            WHERE actif = 1
            """);
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" AND patient_link_status = ?");
      args.add(status.trim().toUpperCase(Locale.ROOT));
    }
    sql.append(" ORDER BY id LIMIT ? OFFSET ?");
    args.add(Math.min(Math.max(limit, 1), 2000));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("matricule", rs.getString("matricule"));
      row.put("nomComplet", rs.getString("nom_complet"));
      row.put("dateNaissance", rs.getObject("date_naissance"));
      row.put("medilineIpp", rs.getString("mediline_ipp"));
      row.put("medilinePatientId", rs.getObject("mediline_patient_id"));
      row.put("patientLinkStatus", rs.getString("patient_link_status"));
      return row;
    }, args.toArray());
  }

  public long countAyantsForMatch(String status) {
    if (status == null || status.isBlank()) {
      return scalar("SELECT COUNT(*) FROM erp_ayants_droit WHERE actif = 1");
    }
    return scalar(
        "SELECT COUNT(*) FROM erp_ayants_droit WHERE actif = 1 AND patient_link_status = ?",
        status.trim().toUpperCase(Locale.ROOT));
  }

  public int applyPatientLink(long ayantId, String ipp, Long medilinePatientId, String status) {
    String st = status == null || status.isBlank() ? "LINKED" : status.trim().toUpperCase(Locale.ROOT);
    return jdbc.update(
        """
            UPDATE erp_ayants_droit
            SET mediline_ipp = ?, mediline_patient_id = ?, patient_link_status = ?,
                dateupdate = NOW(), version = version + 1
            WHERE id = ?
            """,
        ipp,
        medilinePatientId,
        st,
        ayantId);
  }

  public Map<String, Long> patientLinkCounters() {
    Map<String, Long> out = new LinkedHashMap<>();
    jdbc.query(
        """
            SELECT patient_link_status AS st, COUNT(*) AS cnt
            FROM erp_ayants_droit WHERE actif = 1
            GROUP BY patient_link_status
            """,
        (rs, i) -> {
          out.put(rs.getString("st") == null ? "UNLINKED" : rs.getString("st"), rs.getLong("cnt"));
          return null;
        });
    return out;
  }

  public List<Map<String, Object>> listStockCompartmentMappings() {
    return jdbc.query(
        """
            SELECT m.id, m.compartment_code, m.legacy_label, m.fk_pharmacy, m.fk_location,
                   m.mapping_status, m.notes, m.actif, p.designation AS pharmacy_label
            FROM erp_stock_compartment_mappings m
            LEFT JOIN pharmacies p ON p.id = m.fk_pharmacy
            WHERE m.actif = 1
            ORDER BY m.compartment_code
            """,
        (rs, i) -> {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("id", rs.getLong("id"));
          row.put("compartmentCode", rs.getString("compartment_code"));
          row.put("legacyLabel", rs.getString("legacy_label"));
          row.put("pharmacyId", rs.getObject("fk_pharmacy"));
          row.put("locationId", rs.getObject("fk_location"));
          row.put("mappingStatus", rs.getString("mapping_status"));
          row.put("notes", rs.getString("notes"));
          row.put("pharmacyLabel", rs.getString("pharmacy_label"));
          return row;
        });
  }

  public int bindStockCompartment(long id, Long pharmacyId, Long locationId, String notes) {
    String status = pharmacyId == null && locationId == null ? "UNMAPPED" : "MAPPED";
    return jdbc.update(
        """
            UPDATE erp_stock_compartment_mappings
            SET fk_pharmacy = ?, fk_location = ?, mapping_status = ?, notes = ?, dateupdate = NOW()
            WHERE id = ?
            """,
        pharmacyId,
        locationId,
        status,
        notes,
        id);
  }

  private long scalar(String sql, Object... args) {
    Long n = args.length == 0
        ? jdbc.queryForObject(sql, Long.class)
        : jdbc.queryForObject(sql, Long.class, args);
    return n == null ? 0L : n;
  }

  private BigDecimal scalarDecimal(String sql, Object... args) {
    BigDecimal n = args.length == 0
        ? jdbc.queryForObject(sql, BigDecimal.class)
        : jdbc.queryForObject(sql, BigDecimal.class, args);
    return n == null ? BigDecimal.ZERO : n;
  }

  private static void setLong(java.sql.PreparedStatement ps, int idx, Long value) throws java.sql.SQLException {
    if (value == null) {
      ps.setObject(idx, null);
    } else {
      ps.setLong(idx, value);
    }
  }

  private static Long longOrNull(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number number) {
      return number.longValue();
    }
    try {
      String text = String.valueOf(raw).trim();
      return text.isEmpty() ? null : Long.parseLong(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String str(Object raw) {
    if (raw == null) {
      return null;
    }
    String text = String.valueOf(raw).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }

  private static BigDecimal decimalOrZero(Object raw) {
    if (raw == null) {
      return BigDecimal.ZERO;
    }
    if (raw instanceof BigDecimal bd) {
      return bd;
    }
    if (raw instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(raw).trim());
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
  }
}
