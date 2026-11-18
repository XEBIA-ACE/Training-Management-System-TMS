package com.tms.admin.api;

import com.tms.crosscutting.audit.AuditLogEntry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;

/** One page of admin audit entries; paging fields follow {@code PagedTrainerList}. */
public record AuditLogPage(
    List<Item> items, int page, int size, long totalElements, int totalPages) {

  /** One audit entry as exposed to administrators. */
  public record Item(
      UUID id,
      Instant occurredAt,
      String entryType,
      String actor,
      String action,
      String resourceType,
      String resourceId,
      String traceId,
      Map<String, Object> details) {

    static Item from(AuditLogEntry e) {
      return new Item(
          e.getId(),
          e.getOccurredAt(),
          e.getEntryType().name(),
          e.getActor(),
          e.getAction(),
          e.getResourceType(),
          e.getResourceId(),
          e.getTraceId(),
          e.getDetails());
    }
  }

  static AuditLogPage from(Page<AuditLogEntry> page) {
    return new AuditLogPage(
        page.getContent().stream().map(Item::from).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
