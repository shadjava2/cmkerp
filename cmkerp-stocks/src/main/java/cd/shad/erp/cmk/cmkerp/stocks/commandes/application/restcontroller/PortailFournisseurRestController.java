package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.PORTAIL_FOURNISSEUR_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request.PortailOffreDraftRequest;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request.PortailProfilPropositionRequest;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request.PortailUnlockRequest;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.PortailFournisseurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Portail public fournisseur — aucune authentification JWT.
 */
@RestController
@RequestMapping(PORTAIL_FOURNISSEUR_BASE)
@RequiredArgsConstructor
@Tag(name = "Portail fournisseur", description = "Accès public par token + code")
public class PortailFournisseurRestController {

  public static final String SESSION_HEADER = "X-Portail-Session";

  private final PortailFournisseurService portailService;

  @GetMapping("/{publicToken}")
  @Operation(summary = "Métadonnées invitation (sans données concurrentes)")
  public Map<String, Object> meta(@PathVariable String publicToken) {
    return portailService.getInvitationMeta(publicToken);
  }

  @PostMapping("/{publicToken}/unlock")
  public Map<String, Object> unlock(
      @PathVariable String publicToken, @RequestBody PortailUnlockRequest body) {
    return portailService.unlock(publicToken, body != null ? body.getCode() : null);
  }

  @GetMapping("/{publicToken}/demande")
  public Map<String, Object> demande(
      @PathVariable String publicToken, @RequestHeader(value = SESSION_HEADER, required = false) String session) {
    return portailService.getDemande(publicToken, session);
  }

  @PutMapping("/{publicToken}/offre")
  public Map<String, Object> saveDraft(
      @PathVariable String publicToken,
      @RequestHeader(value = SESSION_HEADER, required = false) String session,
      @RequestBody PortailOffreDraftRequest body) {
    return portailService.saveDraft(publicToken, session, body);
  }

  @PostMapping("/{publicToken}/offre/submit")
  public Map<String, Object> submit(
      @PathVariable String publicToken,
      @RequestHeader(value = SESSION_HEADER, required = false) String session,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotenceKey) {
    return portailService.submit(publicToken, session, idempotenceKey);
  }

  @PostMapping("/{publicToken}/reopen-request")
  public ResponseEntity<Void> reopen(
      @PathVariable String publicToken,
      @RequestHeader(value = SESSION_HEADER, required = false) String session,
      @RequestBody(required = false) Map<String, String> body) {
    portailService.requestReopen(publicToken, session, body != null ? body.get("motif") : null);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/{publicToken}/profile-change-request")
  public ResponseEntity<Void> profileChange(
      @PathVariable String publicToken,
      @RequestHeader(value = SESSION_HEADER, required = false) String session,
      @RequestBody PortailProfilPropositionRequest body) {
    portailService.requestProfileChange(publicToken, session, body);
    return ResponseEntity.accepted().build();
  }

  @PostMapping(value = "/{publicToken}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> attachments(
      @PathVariable String publicToken,
      @RequestHeader(value = SESSION_HEADER, required = false) String session,
      @RequestPart("file") MultipartFile file) {
    return portailService.uploadAttachment(publicToken, session, file);
  }
}
