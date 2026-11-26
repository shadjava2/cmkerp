package cd.shad.erp.cmk.cmkerp.gateway.dmi;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.AffiliationCoverageService;
import cd.shad.erp.cmk.cmkerp.gateway.mediline.PatientDuplicateDetectionService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionAuthorizationPort;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/dmi")
public class DmiRegistrationController {

  private static final List<String> CREATE_PERMS = List.of(
      "DMI_RECEPTION_PATIENT_CREATE",
      "DMI_ACCUEIL_NOUVEAU_DOSSIER",
      "DMI_CARDEX_NOUVEAU_DOSSIER");

  private static final List<String> DETECT_PERMS = List.of(
      "DMI_PATIENT_DUPLICATE_DETECT",
      "DMI_RECEPTION_PATIENT_CREATE",
      "DMI_ACCUEIL_NOUVEAU_DOSSIER",
      "DMI_CARDEX_NOUVEAU_DOSSIER");

  private final PatientRegistrationOrchestrator orchestrator;
  private final PatientDuplicateDetectionService duplicateDetection;
  private final AffiliationCoverageService coverageService;
  private final JwtTokenProvider jwtTokenProvider;
  private final PermissionAuthorizationPort permissions;

  public DmiRegistrationController(
      PatientRegistrationOrchestrator orchestrator,
      PatientDuplicateDetectionService duplicateDetection,
      AffiliationCoverageService coverageService,
      JwtTokenProvider jwtTokenProvider,
      PermissionAuthorizationPort permissions) {
    this.orchestrator = orchestrator;
    this.duplicateDetection = duplicateDetection;
    this.coverageService = coverageService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.permissions = permissions;
  }

  @PostMapping("/registrations")
  public ResponseEntity<Map<String, Object>> register(
      Principal principal,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.");
    }
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    requireAny(userId, CREATE_PERMS);
    boolean canOverride = userId != null
        && permissions.hasAnyPermission(userId, List.of("DMI_PATIENT_DUPLICATE_OVERRIDE"));
    Map<String, Object> result = orchestrator.register(principal.getName(), userId, body, canOverride);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/registrations/duplicates")
  public ResponseEntity<Map<String, Object>> duplicates(
      Principal principal,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.");
    }
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    requireAny(userId, DETECT_PERMS);
    @SuppressWarnings("unchecked")
    Map<String, Object> identity = body.get("identity") instanceof Map<?, ?> m
        ? (Map<String, Object>) m
        : null;
    if (identity == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identité obligatoire.");
    }
    String requestId = body.get("requestId") == null ? null : String.valueOf(body.get("requestId"));
    return ResponseEntity.ok(duplicateDetection.analyze(identity, requestId));
  }

  @PostMapping("/registrations/preflight")
  public ResponseEntity<Map<String, Object>> preflight(
      Principal principal,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.");
    }
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    requireAny(userId, CREATE_PERMS);

    @SuppressWarnings("unchecked")
    Map<String, Object> identity = body.get("identity") instanceof Map<?, ?> m
        ? (Map<String, Object>) m
        : Map.of();
    @SuppressWarnings("unchecked")
    Map<String, Object> affiliation = body.get("affiliation") instanceof Map<?, ?> m
        ? (Map<String, Object>) m
        : null;
    boolean skip = affiliation == null || Boolean.TRUE.equals(affiliation.get("skip"));
    String requestId = body.get("requestId") == null ? null : String.valueOf(body.get("requestId"));

    Map<String, Object> coverage = skip
        ? Map.of("complete", true, "errors", List.of(), "warnings", List.of())
        : coverageService.resolve(affiliation, false);

    Map<String, Object> duplicates;
    try {
      duplicates = duplicateDetection.analyze(identity, requestId);
    } catch (ResponseStatusException ex) {
      duplicates = new LinkedHashMap<>();
      duplicates.put("status", "UNAVAILABLE");
      duplicates.put("blockingRequired", true);
      duplicates.put("message", ex.getReason());
      duplicates.put("candidates", List.of());
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("coverage", coverage);
    result.put("duplicates", duplicates);
    result.put(
        "canCreate",
        Boolean.TRUE.equals(coverage.get("complete"))
            && !Boolean.TRUE.equals(duplicates.get("blockingRequired")));
    return ResponseEntity.ok(result);
  }

  private void requireAny(Long userId, List<String> codes) {
    if (userId == null || !permissions.hasAnyPermission(userId, codes)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission insuffisante.");
    }
  }
}
