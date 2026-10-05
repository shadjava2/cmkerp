package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cd.shad.erp.cmk.cmkerp.platform.external.mediline.domain.MedilinePersonneRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.mediline.infrastructure.MedilinePersonneRepositoryStub;

/**
 * Stubs Mediline si {@code cmk.datasource.mediline.url} est vide.
 * Condition inverse de {@link MedilineDataSourceConfig} (pas {@code @ConditionalOnMissingBean}).
 */
@Configuration
@ConditionalOnExpression("'${cmk.datasource.mediline.url:}'.trim().length() == 0")
public class MedilineRepositoryFallbackConfig {

  @Bean
  public MedilinePersonneRepository medilinePersonneRepositoryStub() {
    return new MedilinePersonneRepositoryStub();
  }
}
