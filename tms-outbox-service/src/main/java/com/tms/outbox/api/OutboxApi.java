package com.tms.outbox.api;

import com.tms.outbox.api.dto.OutboxEntryResponse;
import com.tms.outbox.api.dto.OutboxStatistics;
import com.tms.outbox.api.dto.PagedOutboxEntriesResponse;
import com.tms.outbox.api.dto.ProcessorStatus;
import com.tms.outbox.api.dto.RetryResponse;
import com.tms.outbox.api.dto.SkipEntryRequest;
import com.tms.outbox.domain.OutboxStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Outbox Processing Service API v1 (specs/SCAFFOLD-SVC-002/references/SVC-002.md, "OpenAPI
 * specification"). Required scopes are enforced on {@link OutboxAdminController}.
 *
 * <p>GAP: the exported OpenAPI document is truncated after {@code /outbox/stats}; response codes
 * for the processor endpoints follow the "Error handling" section (409 when already
 * running/stopped).
 */
@RequestMapping("/api/v1/outbox")
public interface OutboxApi {

  /** 200 PagedOutboxEntriesResponse; 400/401/403 ErrorResponse. Scope outbox:read. */
  @GetMapping("/entries")
  ResponseEntity<PagedOutboxEntriesResponse> listOutboxEntries(
      @RequestParam(required = false) OutboxStatus status,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant toDate,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size);

  /** 200 OutboxEntryResponse; 401/403/404 ErrorResponse. Scope outbox:read. */
  @GetMapping("/entries/{entryId}")
  ResponseEntity<OutboxEntryResponse> getOutboxEntry(@PathVariable UUID entryId);

  /** 202 RetryResponse; 400/401/403/404 ErrorResponse. Scope outbox:manage. */
  @PostMapping("/entries/{entryId}/retry")
  ResponseEntity<RetryResponse> retryOutboxEntry(@PathVariable UUID entryId);

  /** 200 OutboxEntryResponse; 400/401/403/404 ErrorResponse. Scope outbox:admin. */
  @PostMapping("/entries/{entryId}/skip")
  ResponseEntity<OutboxEntryResponse> skipOutboxEntry(
      @PathVariable UUID entryId, @Valid @RequestBody SkipEntryRequest request);

  /** 200 OutboxStatistics; 401/403 ErrorResponse. GAP: scope assumed outbox:read. */
  @GetMapping("/stats")
  ResponseEntity<OutboxStatistics> getOutboxStatistics(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant toDate);

  /** 202 ProcessorStatus; 409 if already running. GAP: scope assumed outbox:admin. */
  @PostMapping("/processor/start")
  ResponseEntity<ProcessorStatus> startProcessor();

  /** 200 ProcessorStatus; 409 if already stopped. GAP: scope assumed outbox:admin. */
  @PostMapping("/processor/stop")
  ResponseEntity<ProcessorStatus> stopProcessor();

  /** 200 ProcessorStatus. GAP: scope assumed outbox:read. */
  @GetMapping("/processor/status")
  ResponseEntity<ProcessorStatus> getProcessorStatus();
}
