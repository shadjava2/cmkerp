package cd.shad.erp.cmk.cmkerp.gateway.rdv;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping(value = "/api/v1/portail-rdv", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "cmk.rdv.enabled", havingValue = "true")
public class PublicRdvController {
  private static final String COOKIE = "cmk-rdv-session";
  private final PublicRdvService service;
  private final boolean secureCookie;

  PublicRdvController(PublicRdvService service, @Value("${cmk.rdv.secure-cookie:true}") boolean secureCookie) {
    this.service = service;
    this.secureCookie = secureCookie;
  }

  public record LinkRequest(@NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Pattern(regexp = "VISITE|COMPTE") String usage, @Positive Long creneauId) {}
  public record LinkToken(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {}
  public record BookRequest(@NotNull @Positive Long creneauId, @NotNull UUID requestId,
      @NotBlank @Size(max = 100) String nom, @NotBlank @Size(max = 100) String prenom, boolean pourMoi) {}
  public record MoveRequest(@NotNull @Positive Long creneauId) {}
  public record LinkPatient(@NotBlank @Size(max = 25) String ipp, @NotNull @PastOrPresent LocalDate dateNaissance) {}
  public record AccessStatus(String usage, Long creneauId, String email) {}

  @ModelAttribute
  void noStore(HttpServletResponse response) {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
  }

  @GetMapping("/creneaux")
  List<PublicRdvService.Slot> slots() { return service.slots(); }

  @PostMapping(value = "/acces", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-CMK-RDV=1")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void send(@Valid @RequestBody LinkRequest body, HttpServletRequest request) {
    service.sendLink(body.email(), body.usage(), body.creneauId(), request.getRemoteAddr());
  }

  @PostMapping(value = "/verification", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-CMK-RDV=1")
  ResponseEntity<AccessStatus> verify(@Valid @RequestBody LinkToken body) {
    var access = service.verifyLink(body.token());
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
        cookie(access.rawToken(), Duration.ofHours("COMPTE".equals(access.purpose()) ? 12 : 1)).toString())
        .body(new AccessStatus(access.purpose(), access.creneauId(), access.email()));
  }

  @GetMapping("/session")
  AccessStatus session(@CookieValue(value = COOKIE, required = false) String raw) {
    var session = service.session(raw, false);
    return new AccessStatus(session.purpose(), session.creneauId(), session.email());
  }

  @PostMapping(value = "/deconnexion", headers = "X-CMK-RDV=1")
  ResponseEntity<Void> logout(@CookieValue(value = COOKIE, required = false) String raw) {
    service.logout(raw);
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
  }

  @PostMapping(value = "/reservations", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-CMK-RDV=1")
  @ResponseStatus(HttpStatus.CREATED)
  PublicRdvService.Booking book(@CookieValue(value = COOKIE, required = false) String raw,
      @Valid @RequestBody BookRequest body) {
    return service.book(service.session(raw, false), body.creneauId(), body.requestId(), body.nom(), body.prenom(), body.pourMoi());
  }

  @GetMapping("/reservations")
  List<PublicRdvService.Booking> bookings(@CookieValue(value = COOKIE, required = false) String raw) {
    return service.list(service.session(raw, true));
  }

  @GetMapping("/recu")
  ResponseEntity<PublicRdvService.Booking> receipt(@CookieValue(value = COOKIE, required = false) String raw) {
    var booking = service.guestReceipt(service.session(raw, false));
    return booking == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(booking);
  }

  @PostMapping(value = "/reservations/{reference}/annuler", headers = "X-CMK-RDV=1")
  PublicRdvService.Booking cancel(@CookieValue(value = COOKIE, required = false) String raw, @PathVariable String reference) {
    return service.update(service.session(raw, true), reference, null);
  }

  @PostMapping(value = "/reservations/{reference}/deplacer", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-CMK-RDV=1")
  PublicRdvService.Booking move(@CookieValue(value = COOKIE, required = false) String raw, @PathVariable String reference,
      @Valid @RequestBody MoveRequest body) {
    return service.update(service.session(raw, true), reference, body.creneauId());
  }

  @GetMapping("/profil")
  PublicRdvService.Profile profile(@CookieValue(value = COOKIE, required = false) String raw) {
    return service.profile(service.session(raw, true));
  }

  @PostMapping(value = "/profil/mediline", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-CMK-RDV=1")
  PublicRdvService.Profile link(@CookieValue(value = COOKIE, required = false) String raw,
      @Valid @RequestBody LinkPatient body, HttpServletRequest request) {
    return service.linkMediline(service.session(raw, true), body.ipp(), body.dateNaissance(), request.getRemoteAddr());
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> businessError(ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
  }

  @ExceptionHandler(ConcurrencyFailureException.class)
  ResponseEntity<Map<String, String>> concurrencyError() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("message", "Ce créneau est en cours de mise à jour. Réessayez après actualisation."));
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<Map<String, String>> dataError() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("message", "Le service de réservation est momentanément indisponible."));
  }

  private ResponseCookie cookie(String value, Duration duration) {
    return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(secureCookie).sameSite("Strict").path("/")
        .maxAge(duration).build();
  }
}
