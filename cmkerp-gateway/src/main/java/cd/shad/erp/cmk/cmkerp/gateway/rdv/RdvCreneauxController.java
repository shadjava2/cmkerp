package cd.shad.erp.cmk.cmkerp.gateway.rdv;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionAuthorizationPort;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.MedecinMappingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/dmi/rendez-vous")
@ConditionalOnProperty(name = "cmk.rdv.enabled", havingValue = "true")
public class RdvCreneauxController {
  private final PublicRdvService service;
  private final JwtTokenProvider jwt;
  private final PermissionAuthorizationPort permissions;
  private final MedecinMappingService doctors;

  RdvCreneauxController(PublicRdvService service, JwtTokenProvider jwt, PermissionAuthorizationPort permissions,
      MedecinMappingService doctors) {
    this.service = service;
    this.jwt = jwt;
    this.permissions = permissions;
    this.doctors = doctors;
  }

  public record Creneau(@NotNull @Positive Long medecinId, @NotNull @Positive Long serviceId,
      @NotNull @Positive Long lieuId, @NotNull OffsetDateTime debut, @NotNull OffsetDateTime fin) {}

  @GetMapping
  List<PublicRdvService.StaffBooking> list(
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day, HttpServletRequest request) {
    Long actor = AuthTokenExtractor.getCurrentUserId(request, jwt);
    if (permissions.hasPermission(actor, "DMI_RECEPTION_RENDEZ_VOUS")) return service.staffBookings(day, null);
    if (!permissions.hasPermission(actor, "DMI_CABINET_RENDEZ_VOUS_VIEW")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès au registre des rendez-vous refusé.");
    }
    long doctorId = doctors.doctorOfUser(actor).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre compte doit être associé à un médecin.")).id();
    return service.staffBookings(day, doctorId);
  }

  @PostMapping("/creneaux")
  @ResponseStatus(HttpStatus.CREATED)
  void publish(@Valid @RequestBody Creneau slot, HttpServletRequest request) {
    Long actor = AuthTokenExtractor.getCurrentUserId(request, jwt);
    if (!permissions.hasPermission(actor, "DMI_RECEPTION_RENDEZ_VOUS")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Publication de créneau refusée.");
    }
    service.addSlot(slot.medecinId(), slot.serviceId(), slot.lieuId(), slot.debut(), slot.fin(), actor);
  }
}
