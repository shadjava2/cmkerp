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

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.MailingSendDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.MailingSendRequest;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.MailingSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/mailingsend")
@RequiredArgsConstructor
@Validated
@Tag(name = "Stock Intelligence - Mailing Send")
public class MailingSendRestController {

  private final MailingSendService mailingSendService;

  @GetMapping
  @Operation(summary = "Liste tous les destinataires")
  public ResponseEntity<List<MailingSendDTO>> findAll() {
    return ResponseEntity.ok(mailingSendService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<MailingSendDTO> findById(@PathVariable Long id) {
    return ResponseEntity.ok(mailingSendService.findById(id));
  }

  @PostMapping
  public ResponseEntity<MailingSendDTO> create(@Valid @RequestBody MailingSendRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(mailingSendService.create(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<MailingSendDTO> update(
      @PathVariable Long id,
      @Valid @RequestBody MailingSendRequest request) {
    return ResponseEntity.ok(mailingSendService.update(id, request));
  }

  @PatchMapping("/{id}/toggle")
  public ResponseEntity<MailingSendDTO> toggle(
      @PathVariable Long id,
      @RequestBody Map<String, Boolean> body) {
    return ResponseEntity.ok(mailingSendService.toggleActive(id, Boolean.TRUE.equals(body.get("actif"))));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    mailingSendService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
