package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Beans stock intelligence actifs uniquement si {@code cmkerp.stock-intelligence.enabled=true}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(
    prefix = "cmkerp.stock-intelligence",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public @interface ConditionalOnStockIntelligenceEnabled {
}
