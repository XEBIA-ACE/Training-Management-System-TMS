package com.tms.trainer.api;

import com.tms.trainer.application.TrainerAppService;
import com.tms.trainer.domain.TrainerSearchCriteria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trainer REST API. Requires a bearer JWT (SecurityConfig); roles are checked by TrainerAppService
 * via AuthContextProvider.
 *
 * <ul>
 *   <li>US-002 trainer directory: {@code GET /api/trainers} and {@code GET /api/trainers/{id}}
 *       (trainer directory role).
 *   <li>US-003 trainer creation: {@code POST /api/v1/trainers} (TRAINER.MANAGE). The directory
 *       reads are also served under {@code /api/v1/trainers} so a created trainer can be found with
 *       {@code GET /api/v1/trainers?status=ACTIVE} (FR-US-003-005).
 * </ul>
 */
@RestController
@RequestMapping({TrainerController.BASE_PATH, TrainerController.V1_BASE_PATH})
public class TrainerController {

  public static final String BASE_PATH = "/api/trainers";
  public static final String V1_BASE_PATH = "/api/v1/trainers";

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

  /**
   * US-003: 201 with the created ACTIVE trainer and its derived timeZone; 400 for an invalid
   * TrainerCreateRequest, 401/403 without authentication/TRAINER.MANAGE, 409 for a duplicate
   * employeeId or email.
   */
  @PostMapping
  public ResponseEntity<TrainerResponse> create(@RequestBody TrainerCreateRequest request) {
    TrainerResponse created = TrainerResponse.from(service.create(request.toCommand()));
    return ResponseEntity.created(URI.create(V1_BASE_PATH + "/" + created.id())).body(created);
  }
}
