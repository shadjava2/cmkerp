package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(STOCKS_BASE + "/ordonnances")
@RequiredArgsConstructor
@Tag(name = "Stocks - Ordonnances")
public class OrdonnancesRestController {

  private final NamedParameterJdbcTemplate jdbc;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Long fkService, @RequestParam(required = false) String statut) {
    String sql =
        """
        SELECT o.*, ph.designation AS service_nom
        FROM ordonnances o
        LEFT JOIN pharmacies ph ON ph.id = o.fkService
        WHERE (:fkService IS NULL OR o.fkService = :fkService)
          AND (:statut IS NULL OR o.statut = :statut)
        ORDER BY o.datecreate DESC
        LIMIT 500
        """;
    return jdbc.queryForList(
        sql, Map.of("fkService", fkService, "statut", statut));
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable Long id) {
    Map<String, Object> header =
        jdbc.queryForMap(
            """
            SELECT o.*, ph.designation AS service_nom
            FROM ordonnances o
            LEFT JOIN pharmacies ph ON ph.id = o.fkService
            WHERE o.id = :id
            """,
            Map.of("id", id));
    List<Map<String, Object>> lignes =
        jdbc.queryForList("SELECT * FROM lignes_ordonnance WHERE fkOrdonnance = :id", Map.of("id", id));
    return Map.of("ordonnance", header, "lignes", lignes);
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    var params = new MapSqlParameterSource()
        .addValue("fkFiche", body.getOrDefault("fkFiche", "0"))
        .addValue("fkMedecin", body.get("fkMedecin"))
        .addValue("fkService", body.get("fkService"))
        .addValue("codeprescription", body.get("codeprescription"))
        .addValue("statut", body.getOrDefault("statut", "EN EDITION"))
        .addValue("indication", body.get("indication"))
        .addValue("userId", userId);
    var keyHolder = new GeneratedKeyHolder();
    jdbc.update(
        """
        INSERT INTO ordonnances (fkFiche, fkMedecin, fkService, codeprescription, statut, indication,
        usercreateid, userupdateid)
        VALUES (:fkFiche, :fkMedecin, :fkService, :codeprescription, :statut, :indication, :userId, :userId)
        """,
        params,
        keyHolder);
    Number key = keyHolder.getKey();
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", key != null ? key.longValue() : 0));
  }

  @PutMapping("/{id}/statut")
  public ResponseEntity<Void> updateStatut(
      @PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    jdbc.update(
        "UPDATE ordonnances SET statut=:statut, userupdateid=:userId, dateupdate=NOW() WHERE id=:id",
        Map.of("id", id, "statut", body.get("statut"), "userId", userId));
    return ResponseEntity.noContent().build();
  }
}
