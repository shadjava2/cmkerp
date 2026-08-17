package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Module stock-intelligence (services, repositories et REST API).
 * Chargé explicitement via {@code @Import} depuis le gateway (hors scan global).
 */
@Configuration
@EnableConfigurationProperties(StockIntelligenceProperties.class)
@ComponentScan(
    basePackages = "cd.shad.erp.cmk.cmkerp.stocks.stockintelligence",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = StockIntelligenceAutoConfiguration.class))
public class StockIntelligenceAutoConfiguration {
}
