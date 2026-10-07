package cd.shad.erp.cmk.cmkerp.stocks.usage.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.USAGE_EVENTS_BASE;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventBatchRequest;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventPageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.service.AppUsageEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(USAGE_EVENTS_BASE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Usage console", description = "Journal navigation / clics de la console CMK")
public class UsageEventsRestController {

  private final AppUsageEventService appUsageEventService;
  private final JwtTokenProvider jwtTokenProvider;

  @PostMapping
  @Operation(summary = "Enregistrer un lot d'événements d'utilisation")
  public ResponseEntity<Map<String, Object>> track(
      @Valid @RequestBody UsageEventBatchRequest request, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    String username = currentUsername(httpRequest);
    int saved = appUsageEventService.trackBatch(userId, username, request);
    return ResponseEntity.ok(Map.of("saved", saved));
  }

  @GetMapping
  @Operation(summary = "Consulter l'historique d'utilisation (admin id 1)")
  public ResponseEntity<UsageEventPageResponse> list(
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size,
      HttpServletRequest httpRequest) {
    Long requesterId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    String username = currentUsername(httpRequest);
    return ResponseEntity.ok(
        appUsageEventService.list(
            requesterId, username, userId, eventType, q, from, to, page, size));
  }

  private String currentUsername(HttpServletRequest request) {
    String token = AuthTokenExtractor.extractToken(request);
    if (token == null) {
      return null;
    }
    try {
      return jwtTokenProvider.getUsernameFromToken(token);
    } catch (Exception ex) {
      return null;
    }
  }
}
