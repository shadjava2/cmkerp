package cd.shad.erp.cmk.cmkerp.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceAutoConfiguration;

/**
 * Charge le module stock-intelligence (cmkerp-stocks) dans le gateway.
 */
@Configuration
@Import(StockIntelligenceAutoConfiguration.class)
public class StockIntelligenceWebConfiguration {
}
