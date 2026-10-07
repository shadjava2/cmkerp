package cd.shad.erp.cmk.cmkerp.stocks.usage.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventBatchRequest;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventEntryResponse;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventItemRequest;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventPageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.usage.infrastructure.persistence.AppUsageEventRepository;

@Service
public class AppUsageEventService {

  private static final long SCHADRACK_USER_ID = 1L;
  private static final String SCHADRACK_USERNAME = "schadrack";

  private final AppUsageEventRepository repository;

  public AppUsageEventService(AppUsageEventRepository repository) {
    this.repository = repository;
  }

  public int trackBatch(long userId, String username, UsageEventBatchRequest request) {
    List<UsageEventItemRequest> events =
        request.getEvents() == null ? List.of() : request.getEvents();
    if (events.isEmpty()) {
      return 0;
    }
    if (events.size() > 50) {
      throw new BusinessException("Maximum 50 événements par lot");
    }
    return repository.insertBatch(userId, username, events);
  }

  public UsageEventPageResponse list(
      long requesterId,
      String requesterUsername,
      Long filterUserId,
      String eventType,
      String q,
      String from,
      String to,
      int page,
      int size) {
    requireSchadrack(requesterId, requesterUsername);
    int safeSize = Math.max(1, Math.min(size, 200));
    int safePage = Math.max(0, page);
    LocalDateTime fromDt = parseOptionalDateTime(from, false);
    LocalDateTime toDt = parseOptionalDateTime(to, true);
    long total = repository.count(filterUserId, eventType, q, fromDt, toDt);
    List<UsageEventEntryResponse> content =
        repository.findPage(filterUserId, eventType, q, fromDt, toDt, safePage, safeSize);
    return UsageEventPageResponse.builder()
        .totalElements(total)
        .page(safePage)
        .size(safeSize)
        .content(content)
        .build();
  }

  private static void requireSchadrack(long userId, String username) {
    if (userId == SCHADRACK_USER_ID) {
      return;
    }
    String u = username == null ? "" : username.trim().toLowerCase();
    if (SCHADRACK_USERNAME.equals(u)) {
      return;
    }
    throw new BusinessException("Accès réservé à l'administrateur système");
  }

  private static LocalDateTime parseOptionalDateTime(String raw, boolean endOfDay) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      String t = raw.trim();
      if (t.length() == 10) {
        return LocalDateTime.parse(t + (endOfDay ? "T23:59:59" : "T00:00:00"));
      }
      if (t.endsWith("Z") || t.contains("+")) {
        return java.time.OffsetDateTime.parse(t).toLocalDateTime();
      }
      String normalized = t.replace(" ", "T");
      return LocalDateTime.parse(normalized.substring(0, Math.min(19, normalized.length())));
    } catch (DateTimeParseException ex) {
      throw new BusinessException("Date invalide: " + raw);
    }
  }
}
