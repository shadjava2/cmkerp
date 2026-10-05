package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure.CliniqueLookupRepositoryStub;

/**
 * Stubs CLINIQUE si {@code cmk.datasource.clinique.url} est vide.
 */
@Configuration
@ConditionalOnExpression("'${cmk.datasource.clinique.url:}'.trim().length() == 0")
public class CliniqueRepositoryFallbackConfig {

  @Bean
  public CliniqueLookupRepository cliniqueLookupRepositoryStub() {
    return new CliniqueLookupRepositoryStub();
  }
}
