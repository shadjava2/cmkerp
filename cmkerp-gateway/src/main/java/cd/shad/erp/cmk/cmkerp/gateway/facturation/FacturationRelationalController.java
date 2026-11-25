package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

/**
 * APIs relationnelles Facturation : mappings, transactions ERP, dashboard.
 * Source de vérité = tables erp_*. Écritures Beach hors TCONTRAT non activées.
 */
@RestController
@RequestMapping("/api/v1/facturation")
public class FacturationRelationalController {

  private final ErpFacturationRelationalStore store;
  private final JwtTokenProvider jwtTokenProvider;

  public FacturationRelationalController(
      ErpFacturationRelationalStore store, JwtTokenProvider jwtTokenProvider) {
    this.store = store;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @GetMapping("/dashboard/snapshot")
  public ResponseEntity<Map<String, Object>> dashboard(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return ResponseEntity.ok(store.dashboardSnapshot(from, to));
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
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", store.listPrestations(q, safeSize, safePage * safeSize));
    result.put("total", store.countPrestations(q));
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("beachWriteEnabled", false);
    result.put("mode", "ERP_LOCAL_ONLY");
    return ResponseEntity.ok(result);
  }

  @PostMapping("/billing/prestations")
  public ResponseEntity<Map<String, Object>> createPrestation(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.");
    }
    if (body.get("tarifId") == null && body.get("montant") == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tarif ou montant obligatoire.");
    }
    long id = store.insertPrestation(body, userId);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("syncStatus", "LOCAL_ONLY");
    result.put("beachReplication", "DISABLED");
    result.put("legacySignature", store.findLegacyUserCodeForErpUser(userId).orElse(null));
    result.put("legacySignatureReady", store.findLegacyUserCodeForErpUser(userId).isPresent());
    result.put("message", "Prestation enregistrée dans CMK ERP. Réplication CLINIQUE non autorisée.");
    return ResponseEntity.ok(result);
  }

  @GetMapping("/billing/medicaments")
  public ResponseEntity<Map<String, Object>> listMedicaments(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", store.listMedicamentsTx(q, safeSize, safePage * safeSize));
    result.put("total", store.countMedicamentsTx(q));
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("beachWriteEnabled", false);
    result.put("mode", "ERP_LOCAL_ONLY");
    return ResponseEntity.ok(result);
  }

  @PostMapping("/billing/medicaments")
  public ResponseEntity<Map<String, Object>> createMedicament(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.");
    }
    if (body.get("productId") == null && body.get("productCode") == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit obligatoire.");
    }
    long id = store.insertMedicament(body, userId);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("syncStatus", "LOCAL_ONLY");
    result.put("beachReplication", "DISABLED");
    result.put("stockMovement", "NONE");
    result.put("legacySignature", store.findLegacyUserCodeForErpUser(userId).orElse(null));
    result.put("legacySignatureReady", store.findLegacyUserCodeForErpUser(userId).isPresent());
    result.put("message",
        "Médicament enregistré dans CMK ERP. Aucune sortie de stock ni réplication CLINIQUE.");
    return ResponseEntity.ok(result);
  }
}
