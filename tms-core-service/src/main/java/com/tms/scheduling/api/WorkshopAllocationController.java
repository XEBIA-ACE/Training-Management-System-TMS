package com.tms.scheduling.api;

import com.tms.scheduling.application.AllocationService;
import com.tms.scheduling.domain.AllocationStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/v1/workshops/{id}/allocate-trainer (FR-US-002-006). */
@RestController
@RequestMapping("/api/v1/workshops")
public class WorkshopAllocationController {

  private final AllocationService service;

  public WorkshopAllocationController(AllocationService service) {
    this.service = service;
  }

  @PostMapping("/{id}/allocate-trainer")
  public ResponseEntity<AllocationResponse> allocateTrainer(
      @PathVariable UUID id, @Valid @RequestBody AllocateTrainerRequest body) {
    AllocationStatus status = body.status() != null ? body.status() : AllocationStatus.TENTATIVE;
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            AllocationResponse.from(
                service.allocateTrainer(id, body.trainerId(), body.availabilityId(), status)));
  }
}
