package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/facturation")
public class FacturationContratController {

  private final ErpContratService service;
  private final JwtTokenProvider jwtTokenProvider;

  public FacturationContratController(ErpContratService service, JwtTokenProvider jwtTokenProvider) {
    this.service = service;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @GetMapping("/contrats")
  public ResponseEntity<Map<String, Object>> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean actif,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(service.list(q, actif, page, size));
  }

  @GetMapping("/contrats/{id}")
  public ResponseEntity<ErpContratRecord> get(@PathVariable long id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping("/contrats")
  public ResponseEntity<ErpContratRecord> create(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    return ResponseEntity.ok(service.create(stringVal(body.get("code")), stringVal(body.get("designation")), userId));
  }

  @PutMapping("/contrats/{id}")
  public ResponseEntity<ErpContratRecord> update(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    return ResponseEntity.ok(service.update(id, stringVal(body.get("designation")), intVal(body.get("version"), 1), userId));
  }

  @PutMapping("/contrats/{id}/actif")
  public ResponseEntity<ErpContratRecord> setActif(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    boolean actif = Boolean.TRUE.equals(body.get("actif"))
        || "true".equalsIgnoreCase(String.valueOf(body.get("actif")));
    return ResponseEntity.ok(service.setActif(id, actif, intVal(body.get("version"), 1), userId));
  }

  @GetMapping("/contrats/import/candidates")
  public ResponseEntity<Map<String, Object>> importCandidates(
      @RequestParam(required = false) String q) {
    return ResponseEntity.ok(service.listImportCandidates(q));
  }

  @PostMapping("/contrats/import/selected")
  public ResponseEntity<Map<String, Object>> importSelected(@RequestBody Map<String, Object> body) {
    Object raw = body == null ? null : body.get("codes");
    List<String> codes = Collections.emptyList();
    if (raw instanceof List<?> list) {
      codes = list.stream().map(v -> v == null ? null : String.valueOf(v)).toList();
    }
    return ResponseEntity.ok(service.importSelected(codes));
  }

  @PostMapping("/contrats/import/dry-run")
  public ResponseEntity<Map<String, Object>> dryRun() {
    return ResponseEntity.ok(service.dryRunImport());
  }

  @PostMapping("/contrats/import")
  public ResponseEntity<Map<String, Object>> importNow() {
    return ResponseEntity.ok(service.importFromClinic());
  }

  @GetMapping("/clinic-sync/status")
  public ResponseEntity<Map<String, Object>> syncStatus() {
    return ResponseEntity.ok(service.syncStatus());
  }

  private static String stringVal(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static int intVal(Object o, int def) {
    if (o == null) {
      return def;
    }
    if (o instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(o));
    } catch (NumberFormatException e) {
      return def;
    }
  }
}
