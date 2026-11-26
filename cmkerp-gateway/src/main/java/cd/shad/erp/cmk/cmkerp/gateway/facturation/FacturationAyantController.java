package cd.shad.erp.cmk.cmkerp.gateway.facturation;

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
@RequestMapping("/api/v1/facturation/ayants")
public class FacturationAyantController {

  private final ErpAyantService service;
  private final JwtTokenProvider jwtTokenProvider;

  public FacturationAyantController(ErpAyantService service, JwtTokenProvider jwtTokenProvider) {
    this.service = service;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @GetMapping("/meta")
  public ResponseEntity<Map<String, Object>> meta() {
    return ResponseEntity.ok(service.meta());
  }

  /** Lookups ERP MySQL pour le formulaire — hors ClinicReferentialService / CLINIQUE. */
  @GetMapping("/lookups/locations")
  public ResponseEntity<Map<String, Object>> lookupLocations() {
    return ResponseEntity.ok(service.lookupLocations());
  }

  @GetMapping("/lookups/contrats")
  public ResponseEntity<Map<String, Object>> lookupContrats() {
    return ResponseEntity.ok(service.lookupContrats());
  }

  @GetMapping("/lookups/societes")
  public ResponseEntity<Map<String, Object>> lookupSocietes(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    return ResponseEntity.ok(service.lookupSocietes(q, page, size));
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long societeId,
      @RequestParam(required = false) Long contratId,
      @RequestParam(required = false) Long locationId,
      @RequestParam(required = false) Boolean actif,
      @RequestParam(required = false) String patientLinkStatus,
      @RequestParam(required = false) String syncStatus,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(service.list(
        q, societeId, contratId, locationId, actif, patientLinkStatus, syncStatus, page, size));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> get(@PathVariable long id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return ResponseEntity.ok(service.create(body, currentUserId(request)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Map<String, Object>> update(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return ResponseEntity.ok(service.update(id, body, currentUserId(request)));
  }

  @PutMapping("/{id}/actif")
  public ResponseEntity<Map<String, Object>> setActif(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    boolean actif = body.get("actif") instanceof Boolean b
        ? b
        : Boolean.parseBoolean(String.valueOf(body.get("actif")));
    int version = body.get("version") instanceof Number n
        ? n.intValue()
        : Integer.parseInt(String.valueOf(body.get("version")));
    return ResponseEntity.ok(service.setActif(id, actif, version, currentUserId(request)));
  }

  private Long currentUserId(HttpServletRequest request) {
    return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
  }
}
