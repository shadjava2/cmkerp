package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.GMAO_EQUIPEMENTS_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.GMAO_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.EquipementMediaResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service.EquipementMediaService;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service.EquipementMediaService.MediaContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "GMAO — Médias équipements", description = "Photos, documents et vidéos des DM")
public class EquipementMediaRestController {

  private final EquipementMediaService mediaService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping(GMAO_EQUIPEMENTS_BASE + "/{equipementId}/medias")
  @Operation(summary = "Lister les médias d'un équipement")
  public ResponseEntity<List<EquipementMediaResponse>> list(@PathVariable Long equipementId) {
    return ResponseEntity.ok(mediaService.listByEquipement(equipementId));
  }

  @PostMapping(GMAO_EQUIPEMENTS_BASE + "/{equipementId}/medias")
  @Operation(summary = "Uploader un média (photo, PDF, vidéo)")
  public ResponseEntity<EquipementMediaResponse> upload(
      @PathVariable Long equipementId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) String typeMedia,
      @RequestParam(required = false) String legende,
      @RequestParam(required = false) Boolean estPrincipal,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mediaService.upload(equipementId, file, typeMedia, legende, estPrincipal, userId));
  }

  @GetMapping(GMAO_BASE + "/medias/{mediaId}/content")
  @Operation(summary = "Télécharger / afficher le contenu d'un média")
  public ResponseEntity<org.springframework.core.io.Resource> content(@PathVariable Long mediaId) {
    MediaContent content = mediaService.loadContent(mediaId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + content.filename().replace("\"", "") + "\"")
        .contentType(content.contentType())
        .contentLength(content.size())
        .body(content.resource());
  }

  @PutMapping(GMAO_BASE + "/medias/{mediaId}/principal")
  @Operation(summary = "Définir comme photo principale")
  public ResponseEntity<EquipementMediaResponse> setPrincipal(@PathVariable Long mediaId) {
    return ResponseEntity.ok(mediaService.setPrincipal(mediaId));
  }

  @DeleteMapping(GMAO_BASE + "/medias/{mediaId}")
  @Operation(summary = "Supprimer un média")
  public ResponseEntity<Void> delete(@PathVariable Long mediaId) {
    mediaService.delete(mediaId);
    return ResponseEntity.noContent().build();
  }
}
