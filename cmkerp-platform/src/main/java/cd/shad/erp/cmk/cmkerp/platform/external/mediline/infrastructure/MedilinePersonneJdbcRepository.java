package cd.shad.erp.cmk.cmkerp.platform.external.mediline.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.platform.external.mediline.domain.MedilinePersonne;
import cd.shad.erp.cmk.cmkerp.platform.external.mediline.domain.MedilinePersonneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;

/**
 * JDBC Mediline — SELECT uniquement sur {@code t_personne}.
 */
@Repository
@ConditionalOnBean(name = "medilineDataSource")
public class MedilinePersonneJdbcRepository extends AbstractJdbcRepository
    implements MedilinePersonneRepository {

  private static final RowMapper<MedilinePersonne> MAPPER = (rs, rowNum) -> new MedilinePersonne(
      rs.getString("CODE"),
      rs.getString("NOM"),
      rs.getString("POSTNOM"),
      rs.getString("PRENOM"),
      rs.getString("SEXE"));

  public MedilinePersonneJdbcRepository(
      @Qualifier("medilineJdbcTemplate") JdbcTemplate jdbcTemplate,
      @Qualifier("medilineNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
    super(jdbcTemplate, namedJdbcTemplate);
  }

  @Override
  public Optional<MedilinePersonne> findByCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return queryForOptional(
        "SELECT CODE, NOM, POSTNOM, PRENOM, SEXE FROM t_personne WHERE CODE = ? LIMIT 1",
        MAPPER,
        code.trim());
  }

  @Override
  public List<MedilinePersonne> searchByNom(String nomLike, int limit) {
    if (nomLike == null || nomLike.isBlank()) {
      return List.of();
    }
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return queryForList(
        "SELECT CODE, NOM, POSTNOM, PRENOM, SEXE FROM t_personne WHERE NOM LIKE ? ORDER BY NOM LIMIT ?",
        MAPPER,
        "%" + nomLike.trim() + "%",
        safeLimit);
  }

  @Override
  public boolean isAvailable() {
    Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    return one != null && one == 1;
  }
}
