package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_PERIMABLE_ALERTES_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.AddPerimableAlerteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.RetirerStockExpireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.UpdatePerimableAlerteDateRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.PerimableAlerteService;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.PerimableAlerteStockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des alertes de péremption.
 */
@RestController
@RequestMapping({STOCKS_BASE + "/perimable-alertes", POS_PERIMABLE_ALERTES_BASE})
@RequiredArgsConstructor
@Tag(name = "Stocks - Alertes de Péremption", description = "Gestion des alertes de péremption des produits")
@Validated
public class PerimableAlerteRestController {

    private final PerimableAlerteService perimableAlerteService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Ajoute une alerte de péremption avec approv = false.
     */
    @PostMapping("/add")
    @Operation(summary = "Ajoute une alerte de péremption avec approv = false")
    public ResponseEntity<Map<String, Object>> addPerimableAlerte(
            @Valid @RequestBody AddPerimableAlerteRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        Long alerteId = perimableAlerteService.addPerimableAlerte(request, currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", alerteId);
        response.put("message", "Alerte de péremption ajoutée avec succès");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retire du stock périmé en mettant notifactif = false et en définissant stockexpiree.
     * Si la quantité est zéro, désactive automatiquement toutes les alertes de péremption pour ce produit.
     */
    @PostMapping("/retirer-stock-expire")
    @Operation(summary = "Retire du stock périmé et met à jour les alertes")
    public ResponseEntity<Map<String, Object>> retirerStockExpire(
            @Valid @RequestBody RetirerStockExpireRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);

        // Vérifier si le stock est à zéro avant de retirer
        // Cette vérification est faite dans le service, mais on peut aussi la faire ici pour le message
        perimableAlerteService.retirerStockExpire(request, currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Stock expiré retiré avec succès");

        return ResponseEntity.ok(response);
    }

    /**
     * Liste les alertes de péremption actives pour un stock.
     */
  @GetMapping("/stock/{stockId}")
  @Operation(summary = "Liste les alertes de péremption actives d'un stock")
  public ResponseEntity<List<PerimableAlerteStockResponse>> listByStock(@PathVariable Long stockId) {
    List<PerimableAlerteStockResponse> alertes = perimableAlerteService.listActiveByStock(stockId);
    return ResponseEntity.ok(alertes);
  }

  @GetMapping("/list")
  @Operation(summary = "Liste les alertes de péremption par pharmacie")
  public ResponseEntity<List<Map<String, Object>>> listByPharmacie(
      @RequestParam(required = false) Long pharmacieId) {
    return ResponseEntity.ok(perimableAlerteService.listActiveByPharmacie(pharmacieId));
  }

    /**
     * Met à jour la date de péremption d'une alerte active.
     */
    @PutMapping("/{id}/date")
    @Operation(summary = "Modifie la date de péremption d'une alerte active")
    public ResponseEntity<Map<String, Object>> updateDate(@PathVariable Long id,
            @Valid @RequestBody UpdatePerimableAlerteDateRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        perimableAlerteService.updateDate(id, request.getDateperemtion(), currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Date de péremption mise à jour");
        return ResponseEntity.ok(response);
    }

    /**
     * Extrait l'ID de l'utilisateur connecté depuis le JWT token dans l'en-tête Authorization.
     *
     * @param request la requête HTTP
     * @return l'ID de l'utilisateur connecté
     * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si le token est invalide
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }

}





