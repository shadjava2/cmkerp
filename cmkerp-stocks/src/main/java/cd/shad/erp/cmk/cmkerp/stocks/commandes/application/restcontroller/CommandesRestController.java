package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.COMMANDES_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request.*;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response.DashboardCountsResponse;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response.DemandeCotationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response.EnvoiCotationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.CommandesApplicationService;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.ParamScoreFournisseur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(COMMANDES_BASE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Commandes fournisseurs", description = "Cotation → attribution → BC → réception")
public class CommandesRestController {

  private final CommandesApplicationService service;
  private final JwtTokenProvider jwtTokenProvider;

  private Long userId(HttpServletRequest request) {
    return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
  }

  @GetMapping("/dashboard")
  @Operation(summary = "Compteurs dashboard commandes")
  public DashboardCountsResponse dashboard() {
    return service.dashboard();
  }

  /* —— Cotations (alias demandes-cotation) —— */

  @GetMapping({"/cotations", "/demandes-cotation"})
  public PageResponse<DemandeCotationResponse> listCotations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) Long fkPharmacie,
      @RequestParam(required = false) String search) {
    return service.listCotations(page, size, statut, fkPharmacie, search);
  }

  @GetMapping({"/cotations/{id}", "/demandes-cotation/{id}"})
  public DemandeCotationResponse getCotation(@PathVariable Long id) {
    return service.getCotation(id);
  }

  @PostMapping({"/cotations", "/demandes-cotation"})
  public ResponseEntity<DemandeCotationResponse> create(
      @Valid @RequestBody DemandeCotationRequest req, HttpServletRequest http) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createCotation(req, userId(http)));
  }

  @PutMapping({"/cotations/{id}", "/demandes-cotation/{id}"})
  public DemandeCotationResponse update(
      @PathVariable Long id, @Valid @RequestBody DemandeCotationRequest req, HttpServletRequest http) {
    return service.updateCotation(id, req, userId(http));
  }

  @PostMapping({"/cotations/{id}/soumettre-approbation", "/demandes-cotation/{id}/soumettre-approbation"})
  @Operation(summary = "Soumettre la cotation pour approbation interne (BROUILLON → EN_VALIDATION_INTERNE)")
  public DemandeCotationResponse soumettreApprobation(@PathVariable Long id, HttpServletRequest http) {
    return service.soumettreApprobationInterne(id, userId(http));
  }

  @PostMapping({"/cotations/{id}/approuver", "/demandes-cotation/{id}/approuver"})
  @Operation(summary = "Approuver la cotation (validation interne ou manuelle — → APPROUVEE)")
  public DemandeCotationResponse approuver(@PathVariable Long id, HttpServletRequest http) {
    return service.approuverCotation(id, userId(http));
  }

  @PostMapping({"/cotations/{id}/retour-brouillon", "/demandes-cotation/{id}/retour-brouillon"})
  @Operation(summary = "Renvoyer la cotation en brouillon (depuis EN_VALIDATION_INTERNE)")
  public DemandeCotationResponse retourBrouillon(@PathVariable Long id, HttpServletRequest http) {
    return service.retourBrouillonCotation(id, userId(http));
  }

  @PostMapping({"/cotations/{id}/envoyer", "/demandes-cotation/{id}/envoyer"})
  @Operation(summary = "Envoyer aux fournisseurs — nécessite statut APPROUVEE ; génère un mot de passe temporaire unique par invitation")
  public EnvoiCotationResponse envoyer(@PathVariable Long id, HttpServletRequest http) {
    return service.envoyerCotation(id, userId(http));
  }

  @PostMapping({"/cotations/{id}/annuler", "/demandes-cotation/{id}/annuler"})
  public DemandeCotationResponse annuler(
      @PathVariable Long id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest http) {
    String motif = body != null ? body.get("motif") : null;
    return service.annulerCotation(id, motif, userId(http));
  }

  @PostMapping({"/cotations/{cotationId}/invitations/{invitationId}/relancer",
      "/demandes-cotation/{cotationId}/invitations/{invitationId}/relancer"})
  public ResponseEntity<Void> relancer(
      @PathVariable Long cotationId, @PathVariable Long invitationId, HttpServletRequest http) {
    service.relancerInvitation(cotationId, invitationId, userId(http));
    return ResponseEntity.noContent().build();
  }

  @PostMapping({"/cotations/{cotationId}/invitations/{invitationId}/regenerer-acces",
      "/demandes-cotation/{cotationId}/invitations/{invitationId}/regenerer-acces"})
  @Operation(summary = "Régénérer lien + mot de passe temporaire (APPROUVEE/ENVOYEE) — sans e-mail, clair une seule fois")
  public EnvoiCotationResponse.AccesTemporaireFournisseur regenererAcces(
      @PathVariable Long cotationId, @PathVariable Long invitationId, HttpServletRequest http) {
    return service.regenererAccesInvitation(cotationId, invitationId, userId(http));
  }

  @GetMapping({"/cotations/{id}/comparatif", "/demandes-cotation/{id}/comparatif"})
  public Map<String, Object> comparatif(@PathVariable Long id) {
    return service.getComparatif(id);
  }

  @PostMapping({"/cotations/{id}/attribuer", "/demandes-cotation/{id}/attribuer"})
  public Map<String, Object> attribuer(
      @PathVariable Long id, @Valid @RequestBody AttributionRequest req, HttpServletRequest http) {
    return service.attribuer(id, req, userId(http));
  }

  @PostMapping({"/cotations/{id}/generer-bons", "/demandes-cotation/{id}/generer-bons"})
  public List<Map<String, Object>> genererBons(@PathVariable Long id, HttpServletRequest http) {
    return service.genererBonsCommande(id, userId(http));
  }

  /* —— Offres —— */

  @GetMapping("/offres")
  public PageResponse<Map<String, Object>> listOffres(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long fkDemandeCotation,
      @RequestParam(required = false) String statut) {
    return service.listOffres(page, size, fkDemandeCotation, statut);
  }

  @GetMapping("/offres/{id}")
  public Map<String, Object> getOffre(@PathVariable Long id) {
    return service.getOffre(id);
  }

  @PostMapping("/offres/{id}/reouverture")
  public ResponseEntity<Void> decideReouverture(
      @PathVariable Long id, @RequestBody ReouvertureDecisionRequest req, HttpServletRequest http) {
    service.decideReouverture(id, req, userId(http));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/reouvertures-offre")
  public List<Map<String, Object>> listReouvertures(
      @RequestParam(required = false) String statut,
      @RequestParam(defaultValue = "100") int limit) {
    return service.listReouvertures(statut, limit);
  }

  /* —— Bons de commande —— */

  @GetMapping("/bons-commande")
  public PageResponse<Map<String, Object>> listBons(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) Long fkFournisseur,
      @RequestParam(required = false) String search) {
    return service.listBons(page, size, statut, fkFournisseur, search);
  }

  @GetMapping("/bons-commande/{id}")
  public Map<String, Object> getBon(@PathVariable Long id) {
    return service.getBon(id);
  }

  @PostMapping("/bons-commande/{id}/{action}")
  public Map<String, Object> transitionBon(
      @PathVariable Long id, @PathVariable String action, HttpServletRequest http) {
    return service.transitionBon(id, action, userId(http));
  }

  @GetMapping("/bons-commande/{id}/as-approv-draft")
  public Map<String, Object> asApprovDraft(@PathVariable Long id) {
    return service.getBonAsApprovDraft(id);
  }

  @GetMapping("/bons-commande/{id}/receptions")
  public List<Map<String, Object>> listReceptions(@PathVariable Long id) {
    return service.listReceptions(id);
  }

  @PostMapping({"/receptions", "/bons-commande/{bonId}/receptions"})
  public ResponseEntity<Map<String, Object>> createReception(
      @PathVariable(required = false) Long bonId,
      @Valid @RequestBody ReceptionCommandeRequest req,
      HttpServletRequest http) {
    if (bonId != null && req.getFkBonCommande() == null) {
      req.setFkBonCommande(bonId);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createReception(req, userId(http)));
  }

  @GetMapping("/reliquats")
  public PageResponse<Map<String, Object>> reliquats(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.listReliquats(page, size);
  }

  /* —— Évaluations / params / modifs —— */

  @GetMapping("/evaluations")
  public PageResponse<Map<String, Object>> listEvaluations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long fkFournisseur) {
    return service.listEvaluations(page, size, fkFournisseur);
  }

  @PostMapping("/evaluations")
  public ResponseEntity<Map<String, Object>> createEvaluation(
      @Valid @RequestBody EvaluationRequest req, HttpServletRequest http) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createEvaluation(req, userId(http)));
  }

  @GetMapping("/parametres/score")
  public ParamScoreFournisseur getParamScore() {
    return service.getParamScore();
  }

  @PutMapping("/parametres/score")
  public ParamScoreFournisseur updateParamScore(
      @RequestBody ParamScoreFournisseur body, HttpServletRequest http) {
    return service.updateParamScore(body, userId(http));
  }

  @GetMapping("/modifs-fournisseur")
  public PageResponse<Map<String, Object>> listModifs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String statut) {
    return service.listModifs(page, size, statut);
  }

  @PostMapping("/modifs-fournisseur/{id}/decision")
  public Map<String, Object> decideModif(
      @PathVariable Long id, @RequestBody ModifDecisionRequest req, HttpServletRequest http) {
    return service.decideModif(id, req, userId(http));
  }
}
