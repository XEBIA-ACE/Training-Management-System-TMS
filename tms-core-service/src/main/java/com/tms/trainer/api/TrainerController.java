package com.tms.trainer.api;

import com.tms.trainer.application.TrainerAppService;
import com.tms.trainer.domain.TrainerSearchCriteria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trainer directory REST API (US-002) under {@code /api/trainers}. Requires a bearer JWT
 * (SecurityConfig) and a trainer directory role (TrainerAppService, via AuthContextProvider).
 */
@RestController
@RequestMapping(TrainerController.BASE_PATH)
public class TrainerController {

  public static final String BASE_PATH = "/api/trainers";

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;
  static final int MAX_SEARCH_LENGTH = 200;

  private final TrainerAppService service;

  public TrainerController(TrainerAppService service) {
    this.service = service;
  }

  /**
   * FR-US-002-001/002: trainers matching all supplied filters, ordered by name. Page size is capped
   * at {@value #MAX_PAGE_SIZE} to bound query cost (FR-US-002-003).
   */
  @GetMapping
  public PagedTrainerList search(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID certificationId,
      @RequestParam(required = false) String availabilityState,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) Boolean weekendAvailable,
      @RequestParam(required = false) @Size(max = MAX_SEARCH_LENGTH) String search,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
    TrainerSearchCriteria criteria =
        new TrainerSearchCriteria(
            status, certificationId, availabilityState, locationId, weekendAvailable, search);
    return PagedTrainerList.from(service.search(criteria, page, size).map(TrainerResponse::from));
  }

  /** FR-US-002-004: 200 with the trainer, 403 without a directory role, 404 when unknown. */
  @GetMapping("/{id}")
  public TrainerResponse get(@PathVariable UUID id) {
    return TrainerResponse.from(service.get(id));
  }
}
