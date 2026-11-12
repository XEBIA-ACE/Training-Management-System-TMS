package com.tms.outbox.api;

import com.tms.outbox.api.dto.OutboxEntryResponse;
import com.tms.outbox.api.dto.OutboxStatistics;
import com.tms.outbox.api.dto.PagedOutboxEntriesResponse;
import com.tms.outbox.api.dto.ProcessorStatus;
import com.tms.outbox.api.dto.RetryResponse;
import com.tms.outbox.api.dto.SkipEntryRequest;
import com.tms.outbox.application.OutboxMonitoringService;
import com.tms.outbox.application.OutboxProcessingService;
import com.tms.outbox.domain.OutboxEntryFilter;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxStatus;
import com.tms.outbox.infrastructure.scheduling.OutboxScheduler;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** C4 {@code OutboxAdminController}: thin delegation to the application services. */
@RestController
public class OutboxAdminController implements OutboxApi {

  private final OutboxProcessingService outboxProcessingService;
  private final OutboxMonitoringService outboxMonitoringService;
  private final OutboxScheduler outboxScheduler;
  private final OutboxEntryMapper mapper;

  public OutboxAdminController(
      OutboxProcessingService outboxProcessingService,
      OutboxMonitoringService outboxMonitoringService,
      OutboxScheduler outboxScheduler,
      OutboxEntryMapper mapper) {
    this.outboxProcessingService = outboxProcessingService;
    this.outboxMonitoringService = outboxMonitoringService;
    this.outboxScheduler = outboxScheduler;
    this.mapper = mapper;
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_READ)
  public ResponseEntity<PagedOutboxEntriesResponse> listOutboxEntries(
      OutboxStatus status, String eventType, Instant fromDate, Instant toDate, int page, int size) {
    Page<OutboxMessage> result =
        outboxMonitoringService.findEntries(
            new OutboxEntryFilter(status, eventType, fromDate, toDate), page, size);
    return ResponseEntity.ok(
        new PagedOutboxEntriesResponse(
            result.getContent().stream().map(mapper::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.isFirst(),
            result.isLast()));
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_READ)
  public ResponseEntity<OutboxEntryResponse> getOutboxEntry(UUID entryId) {
    return ResponseEntity.ok(mapper.toResponse(outboxMonitoringService.getEntry(entryId)));
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_MANAGE)
  public ResponseEntity<RetryResponse> retryOutboxEntry(UUID entryId) {
    OutboxMessage message = outboxProcessingService.retryMessage(entryId);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(
            new RetryResponse(
                message.getId(),
                message.getStatus().name(),
                "Retry scheduled",
                message.getNextRetryAt()));
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_ADMIN)
  public ResponseEntity<OutboxEntryResponse> skipOutboxEntry(
      UUID entryId, SkipEntryRequest request) {
    return ResponseEntity.ok(
        mapper.toResponse(outboxProcessingService.skipMessage(entryId, request.reason())));
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_READ)
  public ResponseEntity<OutboxStatistics> getOutboxStatistics(Instant fromDate, Instant toDate) {
    return ResponseEntity.ok(outboxMonitoringService.getStatistics(fromDate, toDate));
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_ADMIN)
  public ResponseEntity<ProcessorStatus> startProcessor() {
    if (!outboxScheduler.start()) {
      throw new ProcessorStateConflictException("Outbox processor is already running");
    }
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(outboxScheduler.status());
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_ADMIN)
  public ResponseEntity<ProcessorStatus> stopProcessor() {
    if (!outboxScheduler.stop()) {
      throw new ProcessorStateConflictException("Outbox processor is not running");
    }
    return ResponseEntity.ok(outboxScheduler.status());
  }

  @Override
  @PreAuthorize(OutboxScopes.HAS_READ)
  public ResponseEntity<ProcessorStatus> getProcessorStatus() {
    return ResponseEntity.ok(outboxScheduler.status());
  }
}
