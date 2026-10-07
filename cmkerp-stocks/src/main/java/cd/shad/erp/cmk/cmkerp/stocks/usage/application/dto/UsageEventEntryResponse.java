package cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageEventEntryResponse {
  private Long id;
  private LocalDateTime occurredAt;
  private Long userId;
  private String username;
  private String eventType;
  private String module;
  private String path;
  private String pageTitle;
  private String actionLabel;
  private String element;
  private String howClient;
  private String userAgent;
  private Long pharmacieId;
  private String portal;
  private String detailsJson;
}
