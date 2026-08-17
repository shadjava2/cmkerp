package cd.shad.erp.cmk.cmkerp.platform.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(name = "co.elastic.clients.elasticsearch.ElasticsearchClient")
@ConditionalOnProperty(name = "cmkerp.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

  private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

  private final ElasticsearchProperties properties;
  private final ObjectMapper objectMapper;

  public ElasticsearchConfig(
      @Autowired ElasticsearchProperties properties,
      @Autowired(required = false) ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
  }

  @Bean
  public RestClient restClient() {
    List<String> uris = properties.getUris();
    if (uris == null || uris.isEmpty()) {
      throw new IllegalStateException(
          "cmkerp.elasticsearch.uris doit être configuré avec au moins une URI Elasticsearch");
    }

    HttpHost[] hosts = uris.stream()
        .map(uri -> {
          try {
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
              String[] parts = uri.replaceFirst("^https?://", "").split(":");
              String host = parts[0];
              int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
              String scheme = uri.startsWith("https://") ? "https" : "http";
              return new HttpHost(host, port, scheme);
            } else {
              String[] parts = uri.split(":");
              String host = parts[0];
              int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
              return new HttpHost(host, port, "http");
            }
          } catch (Exception e) {
            log.error("Erreur lors du parsing de l'URI Elasticsearch: {}", uri, e);
            throw new IllegalArgumentException("URI Elasticsearch invalide: " + uri, e);
          }
        })
        .toArray(HttpHost[]::new);

    RestClientBuilder builder = RestClient.builder(hosts);

    builder.setRequestConfigCallback(requestConfigBuilder -> {
      requestConfigBuilder.setConnectTimeout(properties.getConnectionTimeoutMs());
      requestConfigBuilder.setSocketTimeout(properties.getSocketTimeoutMs());
      return requestConfigBuilder;
    });

    builder.setHttpClientConfigCallback(httpClientBuilder -> {
      if (properties.getUsername() != null && !properties.getUsername().isEmpty()) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
      }

      httpClientBuilder.setMaxConnTotal(100);
      httpClientBuilder.setMaxConnPerRoute(100);

      return httpClientBuilder;
    });

    log.info("Client Elasticsearch configuré avec {} nœud(s): {}", hosts.length, uris);
    return builder.build();
  }

  @Bean
  public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
    return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
  }

  @Bean
  public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
    ElasticsearchClient client = new ElasticsearchClient(transport);
    log.info("Client Elasticsearch Java API créé avec succès");
    return client;
  }
}

