package cd.shad.erp.cmk.cmkerp.gateway.dmi;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/dmi")
public class DmiRegistrationController {

  private final PatientRegistrationOrchestrator orchestrator;
  private final JwtTokenProvider jwtTokenProvider;

  public DmiRegistrationController(
      PatientRegistrationOrchestrator orchestrator,
      JwtTokenProvider jwtTokenProvider) {
    this.orchestrator = orchestrator;
    this.jwtTokenProvider = jwtTokenProvider;
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
    Map<String, Object> result = orchestrator.register(principal.getName(), userId, body);
    return ResponseEntity.ok(result);
  }
}