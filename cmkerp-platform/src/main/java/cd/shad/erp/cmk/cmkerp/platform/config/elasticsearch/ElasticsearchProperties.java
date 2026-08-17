package cd.shad.erp.cmk.cmkerp.platform.config.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Propriétés de configuration pour Elasticsearch.
 */
@ConfigurationProperties(prefix = "cmkerp.elasticsearch")
public class ElasticsearchProperties {

  private boolean enabled = false;
  private List<String> uris = new ArrayList<>();
  private String username;
  private String password;
  private String indexName = "cmkerp-audit-events";
  private int connectionTimeoutMs = 5000;
  private int socketTimeoutMs = 60000;
  private int maxRetries = 3;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getUris() {
    return uris;
  }

  public void setUris(List<String> uris) {
    this.uris = uris;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getIndexName() {
    return indexName;
  }

  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  public int getConnectionTimeoutMs() {
    return connectionTimeoutMs;
  }

  public void setConnectionTimeoutMs(int connectionTimeoutMs) {
    this.connectionTimeoutMs = connectionTimeoutMs;
  }

  public int getSocketTimeoutMs() {
    return socketTimeoutMs;
  }

  public void setSocketTimeoutMs(int socketTimeoutMs) {
    this.socketTimeoutMs = socketTimeoutMs;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }
}


