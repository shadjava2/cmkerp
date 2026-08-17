package cd.shad.erp.cmk.cmkerp.platform.config;

import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configuration Jackson pour la sérialisation/désérialisation JSON. - Dates/temps en ISO 8601
 * (2024-01-15T10:30:00) - Support propre de Java Time (LocalDateTime, etc.) - Ignore les champs
 * inconnus côté requêtes.
 */
@Configuration
public class JacksonConfig {

  /**
   * Format de date utilisé pour la sérialisation des LocalDateTime.
   */
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  /**
   * Customisation centralisée de Jackson via le builder Spring Boot.
   */
  @Bean
  public com.fasterxml.jackson.databind.Module javaTimeModule() {
    return new JavaTimeModule();
  }

  @Bean
  public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
    return mapper;
  }
}
