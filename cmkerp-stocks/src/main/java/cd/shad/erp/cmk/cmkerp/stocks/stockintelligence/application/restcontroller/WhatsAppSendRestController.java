package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSendDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSendRequest;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.WhatsAppSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/whatsapp/numbers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Stock Intelligence - WhatsApp Numbers")
public class WhatsAppSendRestController {

  private final WhatsAppSendService whatsAppSendService;

  @GetMapping
  @Operation(summary = "Liste des numéros WhatsApp autorisés")
  public ResponseEntity<List<WhatsAppSendDTO>> findAll() {
    return ResponseEntity.ok(whatsAppSendService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<WhatsAppSendDTO> findById(@PathVariable Long id) {
    return ResponseEntity.ok(whatsAppSendService.findById(id));
  }

  @PostMapping
  public ResponseEntity<WhatsAppSendDTO> create(@Valid @RequestBody WhatsAppSendRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(whatsAppSendService.create(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<WhatsAppSendDTO> update(
      @PathVariable Long id,
      @Valid @RequestBody WhatsAppSendRequest request) {
    return ResponseEntity.ok(whatsAppSendService.update(id, request));
  }

  @PatchMapping("/{id}/toggle")
  public ResponseEntity<WhatsAppSendDTO> toggle(
      @PathVariable Long id,
      @RequestBody Map<String, Boolean> body) {
    return ResponseEntity.ok(whatsAppSendService.toggleActive(id, Boolean.TRUE.equals(body.get("actif"))));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    whatsAppSendService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
