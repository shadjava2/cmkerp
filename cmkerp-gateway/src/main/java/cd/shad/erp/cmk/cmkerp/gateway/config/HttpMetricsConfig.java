package cd.shad.erp.cmk.cmkerp.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import cd.shad.erp.cmk.cmkerp.gateway.metrics.HttpMetricsInterceptor;

/**
 * Configuration pour les métriques HTTP et l'observabilité.
 *
 * <p>
 * Enregistre les intercepteurs pour collecter les métriques HTTP avec corrélation des logs.
 */
@Configuration
public class HttpMetricsConfig implements WebMvcConfigurer {

  private final HttpMetricsInterceptor httpMetricsInterceptor;

  public HttpMetricsConfig(HttpMetricsInterceptor httpMetricsInterceptor) {
    this.httpMetricsInterceptor = httpMetricsInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(httpMetricsInterceptor).addPathPatterns("/api/**")
        .excludePathPatterns("/actuator/**");
  }
}
