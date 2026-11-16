package com.tms.trainer.api;

import com.tms.trainer.application.TrainerService;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trainer REST API (US-003) under {@code /api/v1/trainers}. Access requires an authenticated
 * bearer JWT (SecurityConfig). GAP(US-003): "authorized HR/admin users" names no roles, so no role
 * restriction is applied.
 */
@RestController
@RequestMapping(TrainerController.BASE_PATH)
public class TrainerController {

  public static final String BASE_PATH = "/api/v1/trainers";

  private final CreateTrainerValidator validator;
  private final TrainerService service;

  TrainerController(CreateTrainerValidator validator, TrainerService service) {
    this.validator = validator;
    this.service = service;
  }

  /** FR-US-003-001: always validates first; the service is not reached on validation failure. */
  @PostMapping
  public ResponseEntity<TrainerResponse> create(
      @RequestBody(required = false) CreateTrainerRequest body, Principal principal) {
    var command = validator.validateAndMapCreateTrainer(body);
    TrainerResponse created = TrainerResponse.from(service.create(command, principal.getName()));
    return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id())).body(created);
  }
}
