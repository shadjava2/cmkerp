package cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClinicLegacySyncProperties.class)
public class ClinicLegacySyncAutoConfiguration {}
