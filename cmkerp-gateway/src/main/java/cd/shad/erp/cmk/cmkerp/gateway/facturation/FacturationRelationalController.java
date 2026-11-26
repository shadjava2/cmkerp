package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

/**
 * APIs relationnelles Facturation : mappings, transactions ERP, dashboard.
 * Source de vérité = tables erp_*. Écritures Beach gated par allow-list write-entities
 * (intentions outbox SUSPENDED conservées pour prestations / médicaments).
 */
@RestController
@RequestMapping("/api/v1/facturation")
public class FacturationRelationalController {

  private final ErpFacturationRelationalStore store;
  private final SocieteMedilineMappingService societeMedilineMappingService;
  private final JwtTokenProvider jwtTokenProvider;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;
  private final FacturationDataConsolidationService consolidation;
  private final JdbcTemplate medilineJdbc;

  public FacturationRelationalController(
      ErpFacturationRelationalStore store,
      SocieteMedilineMappingService societeMedilineMappingService,
      JwtTokenProvider jwtTokenProvider,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper,
      FacturationDataConsolidationService consolidation,
      @Autowired(required = false) @Qualifier("medilineJdbcTemplate") JdbcTemplate medilineJdbc) {
    this.store = store;
    this.societeMedilineMappingService = societeMedilineMappingService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
    this.consolidation = consolidation;
    this.medilineJdbc = medilineJdbc;
  }

  @GetMapping("/dashboard/snapshot")
  public ResponseEntity<Map<String, Object>> dashboard(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return ResponseEntity.ok(store.dashboardSnapshot(from, to));
  }

  @GetMapping("/patients/match/stats")
  public ResponseEntity<Map<String, Object>> patientMatchStats() {
    return ResponseEntity.ok(consolidation.patientMatchStats());
  }

  @GetMapping("/patients/match/exceptions")
  public ResponseEntity<Map<String, Object>> patientMatchExceptions(
      @RequestParam(required = false, defaultValue = "REVIEW") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(consolidation.listPatientExceptions(status, page, size));
  }

  @PostMapping("/patients/match/run")
  public ResponseEntity<Map<String, Object>> runPatientMatch(
      @RequestBody(required = false) Map<String, Object> body) {
    int batchSize = body != null && body.get("batchSize") != null
        ? Integer.parseInt(String.valueOf(body.get("batchSize")))
        : 500;
    int maxBatches = body != null && body.get("maxBatches") != null
        ? Integer.parseInt(String.valueOf(body.get("maxBatches")))
        : 20;
    return ResponseEntity.ok(consolidation.runPatientAutoMatch(batchSize, maxBatches));
  }

  @PutMapping("/patients/match/{ayantId}")
  public ResponseEntity<Map<String, Object>> confirmPatientMatch(
      @PathVariable long ayantId, @RequestBody Map<String, Object> body) {
    String ipp = body.get("medilineIpp") == null ? null : String.valueOf(body.get("medilineIpp"));
    Long medilinePatientId = body.get("medilinePatientId") == null
        ? null
        : Long.valueOf(String.valueOf(body.get("medilinePatientId")));
    return ResponseEntity.ok(consolidation.confirmPatientLink(ayantId, ipp, medilinePatientId));
  }

  @PostMapping("/history/prestations/import")
  public ResponseEntity<Map<String, Object>> importHistoricalPrestations(
      @RequestBody(required = false) Map<String, Object> body) {
    return ResponseEntity.ok(consolidation.importHistoricalPrestations(
        str(body, "from"),
        str(body, "to"),
        intOr(body, "limit", 200),
        intOr(body, "offset", 0)));
  }

  @PostMapping("/history/medicaments/import")
  public ResponseEntity<Map<String, Object>> importHistoricalMedicaments(
      @RequestBody(required = false) Map<String, Object> body) {
    return ResponseEntity.ok(consolidation.importHistoricalMedicaments(
        str(body, "from"),
        str(body, "to"),
        intOr(body, "limit", 200),
        intOr(body, "offset", 0)));
  }

  @PostMapping("/history/stock/import")
  public ResponseEntity<Map<String, Object>> importHistoricalStock(
      @RequestBody(required = false) Map<String, Object> body) {
    return ResponseEntity.ok(consolidation.importHistoricalStock(
        intOr(body, "limit", 200),
        intOr(body, "offset", 0)));
  }

  @GetMapping("/stock/compartments")
  public ResponseEntity<Map<String, Object>> stockCompartments() {
    return ResponseEntity.ok(Map.of("items", consolidation.stockCompartmentMappings()));
  }

  @PutMapping("/stock/compartments/{id}")
  public ResponseEntity<Map<String, Object>> bindStockCompartment(
      @PathVariable long id, @RequestBody Map<String, Object> body) {
    Long pharmacyId = body.get("pharmacyId") == null
        ? null
        : Long.valueOf(String.valueOf(body.get("pharmacyId")));
    Long locationId = body.get("locationId") == null
        ? null
        : Long.valueOf(String.valueOf(body.get("locationId")));
    String notes = body.get("notes") == null ? null : String.valueOf(body.get("notes"));
    return ResponseEntity.ok(consolidation.bindStockCompartment(id, pharmacyId, locationId, notes));
  }

  @GetMapping("/adapters/outbound")
  public ResponseEntity<Map<String, Object>> outboundAdapters() {
    return ResponseEntity.ok(consolidation.outboundAdaptersInventory());
  }

  private static String str(Map<String, Object> body, String key) {
    if (body == null || body.get(key) == null) {
      return null;
    }
    String v = String.valueOf(body.get(key)).trim();
    return v.isEmpty() || "null".equalsIgnoreCase(v) ? null : v;
  }

  private static int intOr(Map<String, Object> body, String key, int fallback) {
    if (body == null || body.get(key) == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(String.valueOf(body.get(key)));
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  @GetMapping("/mappings/users")
  public ResponseEntity<Map<String, Object>> userMappings(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", store.listUserMappings(q, status, safeSize, safePage * safeSize));
    result.put("total", store.countUserMappings(q, status));
    result.put("page", safePage);
    result.put("size", safeSize);
    return ResponseEntity.ok(result);
  }

  @PutMapping("/mappings/users/{id}")
  public ResponseEntity<Map<String, Object>> bindUser(
      @PathVariable long id, @RequestBody Map<String, Object> body) {
    Long erpUserId = body.get("erpUserId") == null ? null : Long.valueOf(String.valueOf(body.get("erpUserId")));
    boolean actif = body.get("actif") == null || Boolean.TRUE.equals(body.get("actif"))
        || "true".equalsIgnoreCase(String.valueOf(body.get("actif")));
    int updated = store.bindUserMapping(id, erpUserId, actif);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mapping introuvable.");
    }
    return ResponseEntity.ok(Map.of("id", id, "erpUserId", erpUserId, "actif", actif, "mappingStatus",
        erpUserId == null ? "UNMATCHED" : "MATCHED"));
  }

  @GetMapping("/mappings/pharmacies")
  public ResponseEntity<Map<String, Object>> pharmacyMappings(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", store.listPharmacyMappings(q, status, safeSize, safePage * safeSize));
    result.put("total", store.countPharmacyMappings(q, status));
    result.put("page", safePage);
    result.put("size", safeSize);
    return ResponseEntity.ok(result);
  }

  @PutMapping("/mappings/pharmacies/{id}")
  public ResponseEntity<Map<String, Object>> bindPharmacy(
      @PathVariable long id, @RequestBody Map<String, Object> body) {
    Long erpPharmacyId = body.get("erpPharmacyId") == null
        ? null
        : Long.valueOf(String.valueOf(body.get("erpPharmacyId")));
    boolean actif = body.get("actif") == null || Boolean.TRUE.equals(body.get("actif"))
        || "true".equalsIgnoreCase(String.valueOf(body.get("actif")));
    int updated = store.bindPharmacyMapping(id, erpPharmacyId, actif);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mapping introuvable.");
    }
    return ResponseEntity.ok(Map.of("id", id, "erpPharmacyId", erpPharmacyId, "actif", actif,
        "mappingStatus", erpPharmacyId == null ? "UNMATCHED" : "MATCHED"));
  }

  @GetMapping("/mappings/pharmacies/erp-options")
  public ResponseEntity<List<Map<String, Object>>> erpPharmacies(
      @RequestParam(required = false) String q) {
    return ResponseEntity.ok(store.listErpPharmacies(q, 100));
  }

  @GetMapping("/mappings/societe-comptes")
  public ResponseEntity<Map<String, Object>> societeMedilineMappings(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", store.listSocieteMedilineMappings(q, status, safeSize, safePage * safeSize));
    result.put("total", store.countSocieteMedilineMappings(q, status));
    result.put("page", safePage);
    result.put("size", safeSize);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/mappings/societe-comptes/suggestions")
  public ResponseEntity<Map<String, Object>> societeMedilineSuggestions(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "40") int size) {
    if (medilineJdbc == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mediline indisponible.");
    }
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    List<Map<String, Object>> societes = store.listSocieteMedilineMappings(
        q, "UNMATCHED", safeSize, safePage * safeSize);
    long total = store.countSocieteMedilineMappings(q, "UNMATCHED");
    List<Map<String, Object>> comptes = medilineJdbc.queryForList(
        """
            SELECT CODE, INTITULE, CATEGORIE_PATIENT, ETAT
            FROM t_compte
            WHERE CODE <> ''
            ORDER BY INTITULE, CODE
            LIMIT 4000
            """);
    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> societe : societes) {
      String designation = String.valueOf(societe.getOrDefault("societeDesignation", ""));
      String code = String.valueOf(societe.getOrDefault("societeCode", ""));
      String normDes = normalizeLabel(designation);
      String normCode = normalizeLabel(code);
      List<Map<String, Object>> suggestions = new ArrayList<>();
      for (Map<String, Object> compte : comptes) {
        String compteCode = String.valueOf(compte.getOrDefault("CODE", ""));
        String intitule = String.valueOf(compte.getOrDefault("INTITULE", ""));
        String normIntitule = normalizeLabel(intitule);
        String normCompteCode = normalizeLabel(compteCode);
        int score = 0;
        if (!normCode.isEmpty() && normCode.equals(normCompteCode)) {
          score = 100;
        } else if (!normDes.isEmpty() && normDes.equals(normIntitule)) {
          score = 95;
        } else if (!normDes.isEmpty() && (normIntitule.contains(normDes) || normDes.contains(normIntitule))) {
          score = 80;
        } else {
          score = tokenOverlapScore(normDes, normIntitule);
        }
        if (score < 55) {
          continue;
        }
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("medilineCompteCode", compteCode);
        suggestion.put("medilineCompteIntitule", intitule);
        suggestion.put("categoriePatient", compte.get("CATEGORIE_PATIENT"));
        suggestion.put("score", score);
        suggestions.add(suggestion);
      }
      suggestions.sort(Comparator.comparingInt((Map<String, Object> s) -> ((Number) s.get("score")).intValue()).reversed());
      if (suggestions.size() > 3) {
        suggestions = suggestions.subList(0, 3);
      }
      Map<String, Object> row = new LinkedHashMap<>(societe);
      row.put("suggestions", suggestions);
      items.add(row);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("total", total);
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("validationRequired", true);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/mappings/societe-comptes/{societeId}")
  public ResponseEntity<Map<String, Object>> societeMedilineMapping(@PathVariable long societeId) {
    return ResponseEntity.ok(store.findSocieteMedilineMapping(societeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Société introuvable.")));
  }

  @PutMapping("/mappings/societe-comptes/{societeId}")
  public ResponseEntity<Map<String, Object>> bindSocieteMedilineCompte(
      @PathVariable long societeId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    String compteCode = body.get("medilineCompteCode") == null ? null : String.valueOf(body.get("medilineCompteCode"));
    String compteIntitule = body.get("medilineCompteIntitule") == null
        ? null
        : String.valueOf(body.get("medilineCompteIntitule"));
    if (body.get("clear") != null && Boolean.parseBoolean(String.valueOf(body.get("clear")))) {
      return ResponseEntity.ok(societeMedilineMappingService.clear(societeId));
    }
    return ResponseEntity.ok(societeMedilineMappingService.bind(societeId, compteCode, compteIntitule, userId));
  }

  @GetMapping("/lookups/mediline-categories")
  public ResponseEntity<Map<String, Object>> medilineCategoriesLookup() {
    if (medilineJdbc == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mediline indisponible.");
    }
    List<Map<String, Object>> items = medilineJdbc.query(
        """
            SELECT CODE AS code, INTITULE AS intitule
            FROM t_categorie_patient
            WHERE CODE <> '' AND CODE <> '-1' AND CHAR_LENGTH(CODE) <= 5
            ORDER BY INTITULE, CODE
            LIMIT 200
            """,
        (rs, i) -> {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("code", rs.getString("code") == null ? null : rs.getString("code").trim());
          row.put("intitule", rs.getString("intitule") == null ? null : rs.getString("intitule").trim());
          return row;
        });
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("total", items.size());
    return ResponseEntity.ok(result);
  }

  @GetMapping("/lookups/mediline-comptes")
  public ResponseEntity<Map<String, Object>> medilineCompteLookup(
      @RequestParam(required = false, defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    if (medilineJdbc == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mediline indisponible.");
    }
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    int offset = safePage * safeSize;
    String like = q == null || q.isBlank() ? null : "%" + q.trim() + "%";
    String countSql = """
        SELECT COUNT(*) FROM t_compte WHERE CODE <> ''
        """ + (like == null ? "" : " AND (CODE = ? OR INTITULE LIKE ?)");
    long total = like == null
        ? medilineJdbc.queryForObject(countSql, Long.class)
        : medilineJdbc.queryForObject(countSql, Long.class, q.trim(), like);
    String listSql = """
        SELECT CODE, INTITULE, CATEGORIE_PATIENT, ETAT
        FROM t_compte WHERE CODE <> ''
        """
        + (like == null ? "" : " AND (CODE = ? OR INTITULE LIKE ?)")
        + " ORDER BY INTITULE, CODE LIMIT ? OFFSET ?";
    List<Map<String, Object>> items = like == null
        ? medilineJdbc.queryForList(listSql, safeSize + 1, offset)
        : medilineJdbc.queryForList(listSql, q.trim(), like, safeSize + 1, offset);
    boolean hasMore = items.size() > safeSize;
    if (hasMore) {
      items = items.subList(0, safeSize);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("total", total);
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("hasMore", hasMore);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/products/search")
  public ResponseEntity<Map<String, Object>> products(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    List<Map<String, Object>> items = store.searchProducts(q, safeSize, safePage * safeSize);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("hasMore", items.size() >= safeSize);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/billing/prestations")
  public ResponseEntity<Map<String, Object>> listPrestations(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    boolean beachOn = writeGuard.beachEntityWriteEnabled("TPRESTATION");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", store.listPrestations(q, safeSize, safePage * safeSize));
    result.put("total", store.countPrestations(q));
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("beachWriteEnabled", beachOn);
    result.put("mode", beachOn ? "BEACH_PENDING" : "BEACH_SUSPENDED");
    result.put("writeEntities", writeGuard.properties().getBeach().getWriteEntities());
    return ResponseEntity.ok(result);
  }

  @PostMapping("/billing/prestations")
  @Transactional
  public ResponseEntity<Map<String, Object>> createPrestation(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.");
    }
    if (body.get("tarifId") == null && body.get("montant") == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tarif ou montant obligatoire.");
    }
    writeGuard.assertChWriteForbidden();
    Optional<String> legacyUser = store.findLegacyUserCodeForErpUser(userId);
    String syncStatus = resolveBillingSyncStatus("TPRESTATION", legacyUser.isPresent(), body);
    Map<String, Object> created = store.insertPrestation(body, userId, syncStatus);
    long id = ((Number) created.get("id")).longValue();
    enqueueBillingIntention(
        "TPRESTATION",
        id,
        String.valueOf(created.get("operationId")),
        "INSERT",
        body,
        userId,
        legacyUser.orElse(null),
        syncStatus,
        List.of(
            "IDMVT", "SERIE", "K", "AFACT", "CODECMK", "NDATE", "SOURCE",
            "UTILISATEUR", "CODEPRES", "LIBPRES", "MONTANT"));
    Map<String, Object> result = new LinkedHashMap<>(created);
    result.put("beachReplication", writeGuard.beachEntityWriteEnabled("TPRESTATION") ? "PENDING" : "SUSPENDED");
    result.put("legacySignature", legacyUser.orElse(null));
    result.put("legacySignatureReady", legacyUser.isPresent());
    result.put(
        "message",
        "Prestation enregistrée dans CMK ERP. Intention CLINIQUE journalisée ("
            + syncStatus
            + "). Écriture Beach non autorisée hors liste blanche.");
    return ResponseEntity.ok(result);
  }

  @GetMapping("/billing/medicaments")
  public ResponseEntity<Map<String, Object>> listMedicaments(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    boolean beachOn = writeGuard.beachEntityWriteEnabled("TMEDICAMENT");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", store.listMedicamentsTx(q, safeSize, safePage * safeSize));
    result.put("total", store.countMedicamentsTx(q));
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("beachWriteEnabled", beachOn);
    result.put("mode", beachOn ? "BEACH_PENDING" : "BEACH_SUSPENDED");
    result.put("writeEntities", writeGuard.properties().getBeach().getWriteEntities());
    return ResponseEntity.ok(result);
  }

  @PostMapping("/billing/medicaments")
  @Transactional
  public ResponseEntity<Map<String, Object>> createMedicament(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.");
    }
    if (body.get("productId") == null && body.get("productCode") == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit obligatoire.");
    }
    writeGuard.assertChWriteForbidden();
    Optional<String> legacyUser = store.findLegacyUserCodeForErpUser(userId);
    String syncStatus = resolveBillingSyncStatus("TMEDICAMENT", legacyUser.isPresent(), body);
    Map<String, Object> created = store.insertMedicament(body, userId, syncStatus);
    long id = ((Number) created.get("id")).longValue();
    enqueueBillingIntention(
        "TMEDICAMENT",
        id,
        String.valueOf(created.get("operationId")),
        "INSERT",
        body,
        userId,
        legacyUser.orElse(null),
        syncStatus,
        List.of(
            "IDM", "QTE", "PAU", "MONTANT", "UNITGEST", "AFACT",
            "UTILISATEUR", "CODEPROD", "CODEPHARM"));
    Map<String, Object> result = new LinkedHashMap<>(created);
    result.put("beachReplication", writeGuard.beachEntityWriteEnabled("TMEDICAMENT") ? "PENDING" : "SUSPENDED");
    result.put("stockMovement", "NONE");
    result.put("legacySignature", legacyUser.orElse(null));
    result.put("legacySignatureReady", legacyUser.isPresent());
    result.put(
        "message",
        "Médicament enregistré dans CMK ERP. Intention CLINIQUE journalisée ("
            + syncStatus
            + "). Aucune sortie de stock Beach tant que les règles et la liste blanche ne sont pas validées.");
    return ResponseEntity.ok(result);
  }

  /**
   * Statut métier : PENDING seulement si l'entité est whitelistée ;
   * BLOCKED_MAPPING si signature utilisateur absente ;
   * BLOCKED_RULES si identifiants historiques non résolus (IDMVT/IDM) ;
   * sinon SUSPENDED (écriture temporairement interdite — intention conservée).
   */
  private String resolveBillingSyncStatus(String entityType, boolean legacySignatureReady, Map<String, Object> body) {
    if (!legacySignatureReady) {
      return "BLOCKED_MAPPING";
    }
    boolean historicalIdsReady = body.get("legacyIdmvt") != null || body.get("legacyIdm") != null;
    if (!historicalIdsReady) {
      return "BLOCKED_RULES";
    }
    if (writeGuard.beachEntityWriteEnabled(entityType)) {
      return "PENDING";
    }
    return "SUSPENDED";
  }

  private void enqueueBillingIntention(
      String entityType,
      long entityId,
      String operationId,
      String operationType,
      Map<String, Object> body,
      Long userId,
      String legacyUser,
      String syncStatus,
      List<String> unresolvedColumns) {
    if (store.hasOpenBillingOutbox(entityId, entityType)) {
      return;
    }
    String outboxStatus =
        "PENDING".equals(syncStatus) && writeGuard.beachEntityWriteEnabled(entityType)
            ? "PENDING"
            : "SUSPENDED";
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("operation", operationType);
      payload.put("entityType", entityType);
      payload.put("erpId", entityId);
      payload.put("operationId", operationId);
      payload.put("userId", userId == null ? 0 : userId);
      payload.put("legacyUser", legacyUser);
      payload.put("businessSyncStatus", syncStatus);
      payload.put("unresolvedHistoricalRules", unresolvedColumns);
      payload.put("body", body);
      payload.put(
          "note",
          "Intention conservée. Adapter INSERT Beach non activé hors WRITE_ENTITIES=TCONTRAT. "
              + "Ne pas exécuter tant que IDMVT/IDM, tarifs et signature ne sont pas résolus.");
      store.enqueueBillingOutbox(
          UUID.randomUUID().toString(),
          entityType,
          entityId,
          operationId,
          operationType,
          objectMapper.writeValueAsString(payload),
          UUID.randomUUID().toString(),
          outboxStatus);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Journalisation synchronisation impossible");
    }
  }

  private static String normalizeLabel(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String nfd = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
    return nfd.replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
  }

  private static int tokenOverlapScore(String left, String right) {
    if (left.isEmpty() || right.isEmpty()) {
      return 0;
    }
    String[] a = left.split(" ");
    String[] b = right.split(" ");
    if (a.length == 0 || b.length == 0) {
      return 0;
    }
    int hits = 0;
    for (String token : a) {
      if (token.length() < 3) {
        continue;
      }
      for (String other : b) {
        if (token.equals(other)) {
          hits++;
          break;
        }
      }
    }
    if (hits == 0) {
      return 0;
    }
    int denom = Math.max(a.length, b.length);
    return Math.min(90, (int) Math.round((hits * 100.0) / denom));
  }
}
