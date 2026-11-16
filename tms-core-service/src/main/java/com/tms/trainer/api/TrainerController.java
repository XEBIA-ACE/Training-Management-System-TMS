package com.tms.trainer.api;

import com.tms.crosscutting.monitoring.MonitoringComponent;
import com.tms.trainer.application.TrainerDirectoryEntry;
import com.tms.trainer.application.TrainerSearchService;
import com.tms.trainer.domain.AllocationState;
import com.tms.trainer.domain.TrainerSearchCriteria;
import com.tms.trainer.domain.TrainerStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.event.Level;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trainer directory API (US-002) under {@code /api/v1/trainers}. Access requires an authenticated
 * bearer JWT (SecurityConfig).
 */
@RestController
@RequestMapping(TrainerController.BASE_PATH)
public class TrainerController {

  public static final String BASE_PATH = "/api/v1/trainers";
  public static final String SEARCH_EVENT = "trainer.search.completed";

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;

  private final TrainerSearchService service;
  private final MonitoringComponent monitoring;

  public TrainerController(TrainerSearchService service, MonitoringComponent monitoring) {
    this.service = service;
    this.monitoring = monitoring;
  }

  /**
   * FR-US-002-001..007: searches trainers by the AND of the supplied filters and optional free-text
   * term {@code q}, and logs one structured event per completed (or failed) search.
   *
   * <p>GAP(FR-US-002-001): omitted filters mean "no constraint"; {@code certificationIds} requires
   * all listed certifications. Paging parameters/limits are an implementation choice, as for
   * customers. Requests rejected before reaching this method (e.g. malformed parameter values) are
   * not logged here.
   */
  @GetMapping
  public TrainerPageResponse search(
      @RequestParam(required = false) TrainerStatus status,
      @RequestParam(required = false) List<UUID> certificationIds,
      @RequestParam(required = false) AllocationState allocationState,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) Boolean weekendAvailable,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
      Principal principal) {
    TrainerSearchCriteria criteria =
        new TrainerSearchCriteria(
            status, certificationIds, allocationState, location, weekendAvailable, q);
    long started = System.nanoTime();
    try {
      Page<TrainerDirectoryEntry> result = service.searchTrainers(criteria, page, size);
      logSearch(principal, criteria, "SUCCESS", result.getTotalElements(), started);
      return TrainerPageResponse.from(result);
    } catch (RuntimeException e) {
      logSearch(principal, criteria, "FAILURE", null, started);
      throw e;
    }
  }

  /**
   * FR-US-002-007: logs user, timestamp (added by the monitoring component) and high-level filter
   * criteria. The free-text term is never logged; only whether one was supplied.
   *
   * <p>TODO(FR-US-002-007, gap "exact masking strategy and what is sensitive in free text"):
   * omission is the provisional strategy pending the external logging policy.
   */
  private void logSearch(
      Principal principal,
      TrainerSearchCriteria criteria,
      String outcome,
      Long totalElements,
      long startedNanos) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("user", principal != null ? principal.getName() : "anonymous");
    fields.put("outcome", outcome);
    fields.put("filter.status", criteria.status());
    fields.put(
        "filter.certificationCount",
        criteria.certificationIds() == null ? 0 : criteria.certificationIds().size());
    fields.put("filter.allocationState", criteria.allocationState());
    fields.put("filter.location", criteria.location());
    fields.put("filter.weekendAvailable", criteria.weekendAvailable());
    fields.put("filter.textTermProvided", criteria.hasText());
    fields.put("totalElements", totalElements);
    fields.put("durationMs", Duration.ofNanos(System.nanoTime() - startedNanos).toMillis());
    monitoring.logEvent(
        "SUCCESS".equals(outcome) ? Level.INFO : Level.WARN, SEARCH_EVENT, fields);
  }
}
