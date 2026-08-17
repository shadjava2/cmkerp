package cd.shad.erp.cmk.cmkerp.gateway.config;

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * Garantit un bean {@link JavaMailSender} lorsque {@code spring.mail.host} est défini.
 * Nécessaire car le starter mail est {@code optional} dans cmkerp-platform et peut être
 * absent du classpath selon le mode de lancement (Eclipse vs JAR).
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@Slf4j
public class GatewayMailConfiguration {

  @Bean
  @ConditionalOnMissingBean(JavaMailSender.class)
  public JavaMailSender javaMailSender(MailProperties mailProperties) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    applyProperties(sender, mailProperties);
    log.info("JavaMailSender configuré — host={}, port={}", mailProperties.getHost(), mailProperties.getPort());
    return sender;
  }

  private static void applyProperties(JavaMailSenderImpl sender, MailProperties properties) {
    sender.setHost(properties.getHost());
    if (properties.getPort() != null) {
      sender.setPort(properties.getPort());
    }
    sender.setUsername(properties.getUsername());
    sender.setPassword(properties.getPassword());
    sender.setProtocol(properties.getProtocol());
    if (properties.getDefaultEncoding() != null) {
      sender.setDefaultEncoding(properties.getDefaultEncoding().name());
    }
    if (!properties.getProperties().isEmpty()) {
      Properties javaMailProps = new Properties();
      javaMailProps.putAll(properties.getProperties());
      sender.setJavaMailProperties(javaMailProps);
    }
  }
}
