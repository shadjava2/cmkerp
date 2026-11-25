package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * Consolidation données avant UAT : rapprochement Mediline batch, imports historiques Beach → erp_*,
 * stock compartiments. Aucune écriture SQL Server / Mediline. Aucun outbox sortant sur import histo.
 */
@Service
public class FacturationDataConsolidationService {

  private final ErpFacturationRelationalStore store;
  private final ClinicReferentialLegacyReader legacyReader;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate medilineJdbc;

  public FacturationDataConsolidationService(
      ErpFacturationRelationalStore store,
      ClinicReferentialLegacyReader legacyReader,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper,
      @Autowired(required = false) @Qualifier("medilineJdbcTemplate") JdbcTemplate medilineJdbc) {
    this.store = store;
    this.legacyReader = legacyReader;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
    this.medilineJdbc = medilineJdbc;
  }

  public Map<String, Object> patientMatchStats() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("counters", store.patientLinkCounters());
    out.put("medilineAvailable", medilineJdbc != null);
    out.put("beachAvailable", legacyReader.available());
    return out;
  }

  public Map<String, Object> listPatientExceptions(String status, int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    String st = status == null || status.isBlank() ? "REVIEW" : status;
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("items", store.listAyantsForMatch(st, safeSize, safePage * safeSize));
    out.put("total", store.countAyantsForMatch(st));
    out.put("page", safePage);
    out.put("size", safeSize);
    out.put("counters", store.patientLinkCounters());
    return out;
  }

  @Transactional
  public Map<String, Object> confirmPatientLink(long ayantId, String ipp, Long medilinePatientId) {
    if (ipp == null || ipp.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IPP Mediline obligatoire.");
    }
    int n = store.applyPatientLink(ayantId, ipp.trim(), medilinePatientId, "LINKED");
    if (n == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ayant droit introuvable.");
    }
    return Map.of("id", ayantId, "medilineIpp", ipp.trim(), "patientLinkStatus", "LINKED");
  }

  /**
   * Rapprochement automatique par lots : MATRICULE ↔ IPP Mediline (CODE), sans N×1 queries.
   * Auto-link uniquement si correspondance exacte unique ; ambiguïté → REVIEW ; contradiction → CONFLICT.
   */
  @Transactional
  public Map<String, Object> runPatientAutoMatch(int batchSize, int maxBatches) {
    if (medilineJdbc == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mediline indisponible.");
    }
    int size = Math.min(Math.max(batchSize, 50), 1000);
    int batches = Math.min(Math.max(maxBatches, 1), 50);
    long linked = 0;
    long review = 0;
    long conflict = 0;
    long unchanged = 0;
    long processed = 0;
    for (int b = 0; b < batches; b++) {
      List<Map<String, Object>> ayants = store.listAyantsForMatch("UNLINKED", size, 0);
      if (ayants.isEmpty()) {
        break;
      }
      List<String> keys = ayants.stream()
          .map(a -> trim(a.get("matricule")))
          .filter(Objects::nonNull)
          .distinct()
          .toList();
      Map<String, List<MedilineHit>> byCode = loadMedilineByCodes(keys);
      Map<String, List<MedilineHit>> byAncien = loadMedilineByAncienDossier(keys);
      for (Map<String, Object> ayant : ayants) {
        processed++;
        long id = ((Number) ayant.get("id")).longValue();
        String matricule = trim(ayant.get("matricule"));
        String nom = trim(ayant.get("nomComplet"));
        if (matricule == null) {
          unchanged++;
          continue;
        }
        List<MedilineHit> hits = new ArrayList<>();
        hits.addAll(byCode.getOrDefault(matricule, List.of()));
        hits.addAll(byAncien.getOrDefault(matricule, List.of()));
        hits = dedupeHits(hits);
        if (hits.isEmpty()) {
          unchanged++;
          continue;
        }
        if (hits.size() > 1) {
          store.applyPatientLink(id, null, null, "REVIEW");
          review++;
          continue;
        }
        MedilineHit hit = hits.get(0);
        if (identityContradicts(nom, hit)) {
          store.applyPatientLink(id, hit.ipp(), null, "CONFLICT");
          conflict++;
          continue;
        }
        store.applyPatientLink(id, hit.ipp(), null, "LINKED");
        linked++;
      }
      if (ayants.size() < size) {
        break;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("processed", processed);
    result.put("linked", linked);
    result.put("review", review);
    result.put("conflict", conflict);
    result.put("unchanged", unchanged);
    result.put("counters", store.patientLinkCounters());
    result.put("note", "Associations automatiques limitées aux IPP exacts uniques. Aucune écriture Mediline.");
    return result;
  }

  /** Import historique TPRESTATION → erp_facturation_prestations. Pas d'outbox. */
  @Transactional
  public Map<String, Object> importHistoricalPrestations(String from, String to, int limit, int offset) {
    writeGuard.assertChWriteForbidden();
    Definition def = ClinicReferentialCatalog.byKey("prestations")
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalogue prestations absent."));
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Source CLINIQUE Beach indisponible.");
    }
    int safeLimit = Math.min(Math.max(limit, 1), 500);
    int safeOffset = Math.max(offset, 0);
    long countBefore = store.countPrestations();
    List<Map<String, Object>> rows = pageBeachByDate(def, from, to, safeOffset, safeLimit, "DATEPRES");
    int imported = 0;
    int updated = 0;
    int failed = 0;
    List<String> sampleLegacyIds = new ArrayList<>();
    List<String> failureSamples = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      try {
        Object idmvtRaw = row.get("IDMVT");
        if (idmvtRaw == null) {
          failed++;
          continue;
        }
        String legacyId = String.valueOf(idmvtRaw).trim();
        if (sampleLegacyIds.size() < 5) {
          sampleLegacyIds.add(legacyId);
        }
        String matricule = trim(row.get("MATRICULE"));
        Long ayantId = store.findAyantIdByMatricule(matricule);
        Long societeId = store.findSocieteId(trim(row.get("CODESOC")));
        Long contratId = store.findContratId(trim(row.get("CONTRASOC")));
        String tarifCode = trim(row.get("CODEPRES"));
        Long tarifId = store.findTarifIdByCode(tarifCode);
        Long serviceId = store.findServiceId(trim(row.get("CODESERV")));
        Long imputationId = store.findImputationId(trim(row.get("IMPUTATION")));
        String medCode = trim(row.get("CODEMED"));
        Long medecinId = store.findMedecinId(medCode);
        Long locationId = store.findLocationId(stringOf(row.get("LOCATION")));
        BigDecimal montant = decimal(row.get("MONTANT"));
        Timestamp datePres = timestampOf(row.get("DATEPRES"));
        String fingerprint = sha256(legacyId + "|" + montant + "|" + datePres);
        String snapshot = writeJson(row);
        int n = store.upsertPrestationHistory(
            legacyId,
            legacyId,
            ayantId,
            matricule,
            societeId,
            contratId,
            tarifId,
            tarifCode,
            serviceId,
            imputationId,
            medecinId,
            medCode,
            locationId,
            montant,
            datePres,
            snapshot,
            fingerprint);
        // MySQL CLIENT_FOUND_ROWS : 1 = insert OU update sans changement ; 2 = update modifié.
        if (store.existsPrestationLegacy(legacyId)) {
          if (n == 2) {
            updated++;
          } else {
            imported++;
          }
        } else {
          failed++;
          if (failureSamples.size() < 5) {
            failureSamples.add(legacyId + ":upsertReturned=" + n);
          }
        }
      } catch (RuntimeException ex) {
        failed++;
        if (failureSamples.size() < 5) {
          failureSamples.add(String.valueOf(ex.getMessage()));
        }
      }
    }
    long countAfter = store.countPrestations();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("entity", "TPRESTATION");
    result.put("requested", rows.size());
    result.put("insertedOrFirstWrite", imported);
    result.put("upserted", updated);
    result.put("failed", failed);
    result.put("countBefore", countBefore);
    result.put("countAfter", countAfter);
    result.put("delta", countAfter - countBefore);
    result.put("sampleLegacyIds", sampleLegacyIds);
    result.put("failureSamples", failureSamples);
    result.put("offset", safeOffset);
    result.put("limit", safeLimit);
    result.put("from", from);
    result.put("to", to);
    result.put("outbox", "NONE");
    result.put("beachWrite", false);
    return result;
  }

  /** Import historique TMEDICAMENT → erp_facturation_medicaments. Pas d'outbox, pas de stock double. */
  @Transactional
  public Map<String, Object> importHistoricalMedicaments(String from, String to, int limit, int offset) {
    writeGuard.assertChWriteForbidden();
    Definition def = ClinicReferentialCatalog.byKey("medicaments")
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalogue médicaments absent."));
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Source CLINIQUE Beach indisponible.");
    }
    int safeLimit = Math.min(Math.max(limit, 1), 500);
    int safeOffset = Math.max(offset, 0);
    long countBefore = store.countMedicaments();
    List<Map<String, Object>> rows = pageBeachByDate(def, from, to, safeOffset, safeLimit, "DATEMVT");
    int imported = 0;
    int updated = 0;
    int failed = 0;
    List<String> sampleLegacyIds = new ArrayList<>();
    List<String> failureSamples = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      try {
        Object idmRaw = row.get("IDM");
        if (idmRaw == null) {
          failed++;
          continue;
        }
        String legacyId = String.valueOf(idmRaw).trim();
        if (sampleLegacyIds.size() < 5) {
          sampleLegacyIds.add(legacyId);
        }
        String matricule = trim(row.get("MATRICULE"));
        String productCode = trim(row.get("CODEPROD"));
        String pharmacyCode = trim(row.get("CODEPHARM"));
        String medCode = trim(row.get("CODEMED"));
        int n = store.upsertMedicamentHistory(
            legacyId,
            store.findAyantIdByMatricule(matricule),
            matricule,
            store.findSocieteId(trim(row.get("CODESOC"))),
            store.findContratId(trim(row.get("CONTRASOC"))),
            store.findProductIdByLegacyCode(productCode),
            productCode,
            store.findMedecinId(medCode),
            medCode,
            store.findPharmacyIdByLegacyCode(pharmacyCode),
            pharmacyCode,
            store.findServiceId(trim(row.get("CODESERV"))),
            store.findLocationId(stringOf(row.get("LOCATION"))),
            decimal(row.get("QTE")),
            decimal(row.get("MONTANT")),
            timestampOf(row.get("DATEMVT")),
            writeJson(row),
            sha256(legacyId + "|" + row.get("MONTANT") + "|" + row.get("DATEMVT")));
        if (store.existsMedicamentLegacy(legacyId)) {
          if (n == 2) {
            updated++;
          } else {
            imported++;
          }
        } else {
          failed++;
          if (failureSamples.size() < 5) {
            failureSamples.add(legacyId + ":upsertReturned=" + n);
          }
        }
      } catch (RuntimeException ex) {
        failed++;
        if (failureSamples.size() < 5) {
          failureSamples.add(String.valueOf(ex.getMessage()));
        }
      }
    }
    long countAfter = store.countMedicaments();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("entity", "TMEDICAMENT");
    result.put("requested", rows.size());
    result.put("insertedOrFirstWrite", imported);
    result.put("upserted", updated);
    result.put("failed", failed);
    result.put("countBefore", countBefore);
    result.put("countAfter", countAfter);
    result.put("delta", countAfter - countBefore);
    result.put("sampleLegacyIds", sampleLegacyIds);
    result.put("failureSamples", failureSamples);
    result.put("offset", safeOffset);
    result.put("limit", safeLimit);
    result.put("stockMovement", "NONE");
    result.put("outbox", "NONE");
    result.put("beachWrite", false);
    return result;
  }

  /**
   * Projection TSTOCK → erp_stock_clinique (une ligne par compartiment STOCKn non null).
   * Aucun mapping inventé vers pharmacies.
   */
  public Map<String, Object> importHistoricalStock(int limit, int offset) {
    writeGuard.assertChWriteForbidden();
    Definition def = ClinicReferentialCatalog.byKey("stock")
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalogue stock absent."));
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Source CLINIQUE Beach indisponible.");
    }
    int safeLimit = Math.min(Math.max(limit, 1), 500);
    int safeOffset = Math.max(offset, 0);
    List<Map<String, Object>> rows = legacyReader.page(def, null, safeOffset, safeLimit);
    int rowsRead = 0;
    int compartments = 0;
    for (Map<String, Object> row : rows) {
      rowsRead++;
      String code = trim(row.get("CODE"));
      if (code == null) {
        continue;
      }
      Long productId = store.findProductIdByLegacyCode(code);
      for (int n = 1; n <= 20; n++) {
        BigDecimal qty = decimal(row.get("STOCK" + n));
        if (qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
          continue;
        }
        String compartment = "STOCK" + n;
        store.upsertStockCompartment(
            code,
            productId,
            compartment,
            qty,
            sha256(code + "|" + compartment + "|" + qty));
        compartments++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("entity", "TSTOCK");
    result.put("productsRead", rowsRead);
    result.put("compartmentsUpserted", compartments);
    result.put("offset", safeOffset);
    result.put("limit", safeLimit);
    result.put("mappingPolicy", "Compartiments conservés tels quels — pas d'attribution pharmacie automatique.");
    result.put("beachWrite", false);
    return result;
  }

  public List<Map<String, Object>> stockCompartmentMappings() {
    return store.listStockCompartmentMappings();
  }

  @Transactional
  public Map<String, Object> bindStockCompartment(long id, Long pharmacyId, Long locationId, String notes) {
    int n = store.bindStockCompartment(id, pharmacyId, locationId, notes);
    if (n == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compartiment introuvable.");
    }
    return Map.of("id", id, "pharmacyId", pharmacyId, "locationId", locationId,
        "mappingStatus", pharmacyId == null && locationId == null ? "UNMAPPED" : "MAPPED");
  }

  public Map<String, Object> outboundAdaptersInventory() {
    List<Map<String, Object>> entities = new ArrayList<>();
    entities.add(adapterRow("TCONTRAT", true, true, true, true, "TCONTRAT", "Démontré live"));
    entities.add(adapterRow("TSERVICE", true, false, false, false, null, "Lecture + import ERP seulement"));
    entities.add(adapterRow("TIMPUTATION", true, false, false, false, null, "Lecture + import ERP seulement"));
    entities.add(adapterRow("TLOCATION", true, false, false, false, null, "Lecture + import ERP seulement"));
    entities.add(adapterRow("TSOCIETE", true, false, false, false, null, "Non démontré"));
    entities.add(adapterRow("TCLASSE", true, false, false, false, null, "Non démontré"));
    entities.add(adapterRow("TTARIF", true, false, false, false, null, "Non démontré"));
    entities.add(adapterRow("TAYANT", true, false, false, false, null, "Non démontré"));
    entities.add(adapterRow("TPRESTATION", true, false, false, false, null,
        "Intention outbox possible ; worker n'exécute pas ; règles IDMVT ouvertes"));
    entities.add(adapterRow("TMEDICAMENT", true, false, false, false, null,
        "Intention outbox possible ; worker n'exécute pas ; règles IDM ouvertes"));
    entities.add(adapterRow("TSTOCK", true, false, false, false, null,
        "Projection + compartiments ; STOCK1..n non mappés automatiquement"));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("writeEntities", writeGuard.properties().getBeach().getWriteEntities());
    out.put("beachWriteEnabled", writeGuard.beachWriteEnabled());
    out.put("chWriteEnabled", writeGuard.chWriteEnabled());
    out.put("workerExecutes", List.of("TCONTRAT"));
    out.put("entities", entities);
    out.put("note", "Une intention SUSPENDED ≠ adaptateur INSERT Beach opérationnel.");
    return out;
  }

  private List<Map<String, Object>> pageBeachByDate(
      Definition def, String from, String to, int offset, int limit, String dateColumn) {
    if (from == null && to == null) {
      return legacyReader.page(def, null, offset, limit);
    }
    // Filtre date côté SQL Server via page générique + post-filtre si colonnes absentes du reader étendu.
    // Pour rester sûr sans DDL : pagination catalogue + filtre mémoire sur le lot.
    List<Map<String, Object>> page = legacyReader.page(def, null, offset, limit);
    LocalDate fromDate = parseDate(from);
    LocalDate toDate = parseDate(to);
    if (fromDate == null && toDate == null) {
      return page;
    }
    return page.stream().filter(row -> {
      LocalDate d = localDateOf(row.get(dateColumn));
      if (d == null) {
        return true;
      }
      if (fromDate != null && d.isBefore(fromDate)) {
        return false;
      }
      if (toDate != null && d.isAfter(toDate)) {
        return false;
      }
      return true;
    }).toList();
  }

  private Map<String, List<MedilineHit>> loadMedilineByCodes(List<String> codes) {
    Map<String, List<MedilineHit>> map = new LinkedHashMap<>();
    if (codes.isEmpty()) {
      return map;
    }
    for (int i = 0; i < codes.size(); i += 200) {
      List<String> chunk = codes.subList(i, Math.min(i + 200, codes.size()));
      String placeholders = chunk.stream().map(c -> "?").collect(Collectors.joining(","));
      try {
        medilineJdbc.query(
            "SELECT CODE, NOM, POSTNOM, PRENOM, DATE_NAISSANCE, ANCIEN_DOSSIER FROM t_personne "
                + "WHERE PERSONNE_PHYSIQUE = 1 AND CODE IN (" + placeholders + ")",
            rs -> {
              MedilineHit hit = new MedilineHit(
                  rs.getString("CODE"),
                  joinName(rs.getString("NOM"), rs.getString("POSTNOM"), rs.getString("PRENOM")),
                  rs.getObject("DATE_NAISSANCE", LocalDate.class),
                  rs.getString("ANCIEN_DOSSIER"));
              map.computeIfAbsent(hit.ipp(), k -> new ArrayList<>()).add(hit);
            },
            chunk.toArray());
      } catch (DataAccessException ex) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Lecture Mediline impossible.");
      }
    }
    return map;
  }

  private Map<String, List<MedilineHit>> loadMedilineByAncienDossier(List<String> keys) {
    Map<String, List<MedilineHit>> map = new LinkedHashMap<>();
    if (keys.isEmpty()) {
      return map;
    }
    for (int i = 0; i < keys.size(); i += 200) {
      List<String> chunk = keys.subList(i, Math.min(i + 200, keys.size()));
      String placeholders = chunk.stream().map(c -> "?").collect(Collectors.joining(","));
      try {
        medilineJdbc.query(
            "SELECT CODE, NOM, POSTNOM, PRENOM, DATE_NAISSANCE, ANCIEN_DOSSIER FROM t_personne "
                + "WHERE PERSONNE_PHYSIQUE = 1 AND ANCIEN_DOSSIER IN (" + placeholders + ")",
            rs -> {
              String ancien = rs.getString("ANCIEN_DOSSIER");
              if (ancien == null || ancien.isBlank()) {
                return;
              }
              MedilineHit hit = new MedilineHit(
                  rs.getString("CODE"),
                  joinName(rs.getString("NOM"), rs.getString("POSTNOM"), rs.getString("PRENOM")),
                  rs.getObject("DATE_NAISSANCE", LocalDate.class),
                  ancien.trim());
              map.computeIfAbsent(ancien.trim(), k -> new ArrayList<>()).add(hit);
            },
            chunk.toArray());
      } catch (DataAccessException ex) {
        // ANCIEN_DOSSIER peut être absent / non indexé — ignorer ce canal.
        return map;
      }
    }
    return map;
  }

  private static List<MedilineHit> dedupeHits(List<MedilineHit> hits) {
    Map<String, MedilineHit> byIpp = new LinkedHashMap<>();
    for (MedilineHit hit : hits) {
      if (hit.ipp() != null) {
        byIpp.putIfAbsent(hit.ipp(), hit);
      }
    }
    return new ArrayList<>(byIpp.values());
  }

  private static boolean identityContradicts(String ayantNom, MedilineHit hit) {
    if (ayantNom == null || ayantNom.isBlank() || hit.nomComplet() == null || hit.nomComplet().isBlank()) {
      return false;
    }
    String a = normalizeName(ayantNom);
    String b = normalizeName(hit.nomComplet());
    if (a.equals(b) || a.contains(b) || b.contains(a)) {
      return false;
    }
    // Si un token significatif du nom ayant apparaît dans Mediline → cohérent
    for (String token : a.split(" ")) {
      if (token.length() >= 3 && b.contains(token)) {
        return false;
      }
    }
    return true;
  }

  private static String normalizeName(String raw) {
    return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9àâäéèêëïîôùûüç\\s]", " ").replaceAll("\\s+", " ").trim();
  }

  private static String joinName(String nom, String postnom, String prenom) {
    StringBuilder sb = new StringBuilder();
    if (nom != null && !nom.isBlank()) {
      sb.append(nom.trim());
    }
    if (postnom != null && !postnom.isBlank()) {
      if (!sb.isEmpty()) {
        sb.append(' ');
      }
      sb.append(postnom.trim());
    }
    if (prenom != null && !prenom.isBlank()) {
      if (!sb.isEmpty()) {
        sb.append(' ');
      }
      sb.append(prenom.trim());
    }
    return sb.toString();
  }

  private static Map<String, Object> adapterRow(
      String entity, boolean reader, boolean insert, boolean update, boolean worker, String whitelist, String note) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("entity", entity);
    row.put("legacyReader", reader);
    row.put("insertAdapter", insert);
    row.put("updateAdapter", update);
    row.put("workerCapable", worker);
    row.put("productionWhitelist", whitelist);
    row.put("note", note);
    return row;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  private static String sha256(String raw) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      return Integer.toHexString(raw.hashCode());
    }
  }

  private static String trim(Object raw) {
    if (raw == null) {
      return null;
    }
    String t = String.valueOf(raw).trim();
    return t.isEmpty() || "null".equalsIgnoreCase(t) ? null : t;
  }

  private static String stringOf(Object raw) {
    return trim(raw);
  }

  private static BigDecimal decimal(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof BigDecimal bd) {
      return bd;
    }
    if (raw instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(raw).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static Timestamp timestampOf(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Timestamp ts) {
      return ts;
    }
    if (raw instanceof java.util.Date d) {
      return new Timestamp(d.getTime());
    }
    String text = String.valueOf(raw).trim().replace('T', ' ');
    if (text.length() == 10) {
      text = text + " 00:00:00";
    } else if (text.length() > 19) {
      text = text.substring(0, 19);
    }
    try {
      return Timestamp.valueOf(text);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static LocalDate parseDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(raw.trim().substring(0, Math.min(10, raw.trim().length())));
    } catch (Exception ex) {
      return null;
    }
  }

  private static LocalDate localDateOf(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof LocalDate ld) {
      return ld;
    }
    if (raw instanceof Date d) {
      return d.toLocalDate();
    }
    if (raw instanceof java.util.Date d) {
      return new Date(d.getTime()).toLocalDate();
    }
    return parseDate(String.valueOf(raw));
  }

  private record MedilineHit(String ipp, String nomComplet, LocalDate dateNaissance, String ancienDossier) {
  }
}
