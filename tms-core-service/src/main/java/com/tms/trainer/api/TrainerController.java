package com.tms.trainer.api;

import com.tms.trainer.application.TrainerAppService;
import com.tms.trainer.application.TrainerCommandService;
import com.tms.trainer.application.TrainerCommandService.CreateCommand;
import com.tms.trainer.application.TrainerCommandService.UpdateCommand;
import com.tms.trainer.domain.TrainerSearchCriteria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trainer directory REST API (US-002) under {@code /api/trainers}. Requires a bearer JWT
 * (SecurityConfig) and a trainer directory role (TrainerAppService, via AuthContextProvider).
 */
@RestController
@RequestMapping({TrainerController.BASE_PATH, TrainerController.V1_BASE_PATH})
public class TrainerController {

  public static final String BASE_PATH = "/api/trainers";

  /**
   * US-003 paths are specified under {@code /api/v1/trainers}; the US-002 reads keep {@code
   * /api/trainers} and are additionally served here (same handlers, no new contract).
   */
  public static final String V1_BASE_PATH = "/api/v1/trainers";

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;
  static final int MAX_SEARCH_LENGTH = 200;

  private final TrainerAppService service;
  private final TrainerCommandService commands;

  public TrainerController(TrainerAppService service, TrainerCommandService commands) {
    this.service = service;
    this.commands = commands;
  }

  /**
   * FR-US-003-001..007: 201 with the ACTIVE trainer; 400 with field errors; 409 on a duplicate
   * employeeId/email; 401 without a token (SecurityConfig); 403 without a trainer create role.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TrainerResponse create(@RequestBody CreateTrainerRequest body) {
    return TrainerResponse.from(
        commands.create(
            new CreateCommand(
                body.type(),
                body.employeeId(),
                body.name(),
                body.email(),
                body.locationId(),
                body.weekendAvailable(),
                body.msaNdaStatus())));
  }

  /** FR-US-003-004: a changed locationId recalculates Trainer.timezone. */
  @PutMapping("/{id}")
  public TrainerResponse update(@PathVariable UUID id, @RequestBody UpdateTrainerRequest body) {
    return TrainerResponse.from(
        commands.update(id, new UpdateCommand(body.name(), body.locationId())));
  }

  /**
   * FR-US-004-AC1-001..003/AC6-001/002: 200 with the full updated trainer; 400 on an invalid
   * status; 403 without HR_ADMIN/SUPER_ADMIN; 404 when unknown.
   */
  @PatchMapping("/{id}/status")
  public TrainerResponse changeStatus(
      @PathVariable UUID id, @RequestBody UpdateTrainerStatusRequest body) {
    return TrainerResponse.from(commands.changeStatus(id, body.status()));
  }

  /** FR-US-003-006/007: immutable audit trail for the trainer. */
  @GetMapping("/{id}/audit-logs")
  public List<TrainerAuditLogResponse> auditLogs(@PathVariable UUID id) {
    return commands.auditLogs(id).stream().map(TrainerAuditLogResponse::from).toList();
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
