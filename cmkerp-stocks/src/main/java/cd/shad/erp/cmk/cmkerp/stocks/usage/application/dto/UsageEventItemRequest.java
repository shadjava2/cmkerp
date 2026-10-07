package cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsageEventItemRequest {

  @NotBlank
  @Size(max = 20)
  private String eventType;

  @Size(max = 60)
  private String module;

  @Size(max = 500)
  private String path;

  @Size(max = 255)
  private String pageTitle;

  @Size(max = 255)
  private String actionLabel;

  @Size(max = 120)
  private String element;

  @Size(max = 40)
  private String howClient;

  @Size(max = 512)
  private String userAgent;

  private Long pharmacieId;

  @Size(max = 40)
  private String portal;

  @Size(max = 4000)
  private String detailsJson;

  /** Horodatage client ISO-8601 (optionnel) ; sinon NOW() serveur. */
  private String occurredAt;
}
