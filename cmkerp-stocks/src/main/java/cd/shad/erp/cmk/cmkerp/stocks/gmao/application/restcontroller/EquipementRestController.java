package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.GMAO_EQUIPEMENTS_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.EquipementRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.EquipementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service.EquipementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(GMAO_EQUIPEMENTS_BASE)
@RequiredArgsConstructor
@Validated
@Tag(name = "GMAO — Équipements", description = "Parc d'équipements à maintenir")
public class EquipementRestController {

  private final EquipementService equipementService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping
  @Operation(summary = "Liste paginée des équipements")
  public ResponseEntity<PageResponse<EquipementResponse>> findAll(
      @RequestParam(required = false) Long fkPharmacie,
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) String categorie,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean actif,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(
        equipementService.findAll(fkPharmacie, statut, categorie, search, actif, page, size));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Détail d'un équipement")
  public ResponseEntity<EquipementResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(equipementService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Créer un équipement")
  public ResponseEntity<EquipementResponse> create(@Valid @RequestBody EquipementRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(equipementService.create(request, userId));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Mettre à jour un équipement")
  public ResponseEntity<EquipementResponse> update(@PathVariable Long id,
      @Valid @RequestBody EquipementRequest request, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(equipementService.update(id, request, userId));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Désactiver un équipement (soft delete)")
  public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    equipementService.softDelete(id, userId);
    return ResponseEntity.noContent().build();
  }
}
