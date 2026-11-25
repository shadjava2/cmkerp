package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
@RequestMapping("/api/v1/facturation/referentials")
public class FacturationReferentialController {

  private final ClinicReferentialService service;
  private final JwtTokenProvider jwtTokenProvider;

  public FacturationReferentialController(
      ClinicReferentialService service, JwtTokenProvider jwtTokenProvider) {
    this.service = service;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @GetMapping("/{key}")
  public ResponseEntity<Map<String, Object>> list(
      @PathVariable String key,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean actif,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(service.list(key, q, actif, page, size));
  }

  @GetMapping("/{key}/{id}")
  public ResponseEntity<Map<String, Object>> get(@PathVariable String key, @PathVariable long id) {
    return ResponseEntity.ok(service.get(key, id));
  }

  @PostMapping("/{key}")
  public ResponseEntity<Map<String, Object>> create(
      @PathVariable String key, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    return ResponseEntity.ok(service.create(key, stringVal(body.get("key")), fieldsOf(body), userId));
  }

  @PutMapping("/{key}/{id}")
  public ResponseEntity<Map<String, Object>> update(
      @PathVariable String key,
      @PathVariable long id,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    return ResponseEntity.ok(service.update(key, id, fieldsOf(body), intVal(body.get("version"), 1), userId));
  }

  @PutMapping("/{key}/{id}/actif")
  public ResponseEntity<Map<String, Object>> setActif(
      @PathVariable String key,
      @PathVariable long id,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    boolean actif = Boolean.TRUE.equals(body.get("actif"))
        || "true".equalsIgnoreCase(String.valueOf(body.get("actif")));
    return ResponseEntity.ok(service.setActif(key, id, actif, intVal(body.get("version"), 1), userId));
  }

  @GetMapping("/{key}/erp-sites")
  public ResponseEntity<List<Map<String, Object>>> erpSites(@PathVariable String key) {
    return ResponseEntity.ok(service.listErpSites(key));
  }

  @GetMapping("/{key}/import/candidates")
  public ResponseEntity<Map<String, Object>> importCandidates(
      @PathVariable String key,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(service.listImportCandidates(key, q, page, size));
  }

  @PostMapping("/{key}/import/selected")
  public ResponseEntity<Map<String, Object>> importSelected(
      @PathVariable String key, @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(service.importSelected(key, keysOf(body)));
  }

  @PostMapping("/{key}/import/dry-run")
  public ResponseEntity<Map<String, Object>> dryRun(@PathVariable String key) {
    return ResponseEntity.ok(service.dryRun(key));
  }

  @PostMapping("/{key}/import")
  public ResponseEntity<Map<String, Object>> importSelectedExplicit(
      @PathVariable String key, @RequestBody(required = false) Map<String, Object> body) {
    return ResponseEntity.ok(service.importSelected(key, keysOf(body)));
  }

  private static Map<String, Object> fieldsOf(Map<String, Object> body) {
    if (body == null) {
      return Map.of();
    }
    Object raw = body.get("fields");
    if (!(raw instanceof Map<?, ?> map)) {
      return Map.of();
    }
    Map<String, Object> fields = new LinkedHashMap<>();
    map.forEach((k, v) -> {
      if (k != null) {
        fields.put(String.valueOf(k), v);
      }
    });
    return fields;
  }

  private static List<String> keysOf(Map<String, Object> body) {
    if (body == null) {
      return List.of();
    }
    Object raw = body.get("keys");
    if (raw == null) {
      raw = body.get("codes");
    }
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    List<String> keys = new ArrayList<>();
    for (Object value : list) {
      keys.add(value == null ? null : String.valueOf(value));
    }
    return keys;
  }

  private static String stringVal(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static int intVal(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }
}
