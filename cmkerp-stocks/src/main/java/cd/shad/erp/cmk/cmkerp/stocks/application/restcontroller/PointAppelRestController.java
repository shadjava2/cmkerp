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
@RequestMapping(STOCKS_BASE + "/point-appel")
@RequiredArgsConstructor
@Tag(name = "Stocks - Point d'appel")
public class PointAppelRestController {

  private final NamedParameterJdbcTemplate jdbc;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(required = false) Long fkService) {
    String sql =
        """
        SELECT pa.*, ph.designation AS service_nom
        FROM point_appel pa
        LEFT JOIN pharmacies ph ON ph.id = pa.fkService
        WHERE (:fkService IS NULL OR pa.fkService = :fkService)
        ORDER BY pa.datecreate DESC
        LIMIT 500
        """;
    return jdbc.queryForList(sql, Map.of("fkService", fkService));
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable Long id) {
    return jdbc.queryForMap(
        "SELECT pa.*, ph.designation AS service_nom FROM point_appel pa "
            + "LEFT JOIN pharmacies ph ON ph.id = pa.fkService WHERE pa.id = :id",
        Map.of("id", id));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    var params = new MapSqlParameterSource()
        .addValue("designation", body.get("designation"))
        .addValue("message", body.get("message"))
        .addValue("fkService", body.get("fkService"))
        .addValue("cabinet", body.get("cabinet"))
        .addValue("userId", userId);
    var keyHolder = new GeneratedKeyHolder();
    jdbc.update(
        """
        INSERT INTO point_appel (designation, message, fkService, cabinet, usercreateid, userupdateid)
        VALUES (:designation, :message, :fkService, :cabinet, :userId, :userId)
        """,
        params,
        keyHolder);
    Number key = keyHolder.getKey();
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", key != null ? key.longValue() : 0));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Void> update(
      @PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    jdbc.update(
        """
        UPDATE point_appel SET designation=:designation, message=:message, fkService=:fkService,
        cabinet=:cabinet, userupdateid=:userId, dateupdate=NOW() WHERE id=:id
        """,
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("designation", body.get("designation"))
            .addValue("message", body.get("message"))
            .addValue("fkService", body.get("fkService"))
            .addValue("cabinet", body.get("cabinet"))
            .addValue("userId", userId));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    jdbc.update("DELETE FROM point_appel WHERE id = :id", Map.of("id", id));
    return ResponseEntity.noContent().build();
  }
}
