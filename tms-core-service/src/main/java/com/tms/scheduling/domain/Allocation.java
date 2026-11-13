package com.tms.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Trainer allocation to a workshop; the date range is copied from the workshop. */
@Entity
@Table(name = "allocation")
public class Allocation {

  @Id private UUID id;

  @Column(name = "workshop_id", nullable = false)
  private UUID workshopId;

  @Column(name = "trainer_id", nullable = false)
  private UUID trainerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AllocationStatus status;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Allocation() {}

  public Allocation(Workshop workshop, UUID trainerId, AllocationStatus status, Instant now) {
    this.id = UUID.randomUUID();
    this.workshopId = workshop.getId();
    this.trainerId = trainerId;
    this.status = status;
    this.startDate = workshop.getStartDate();
    this.endDate = workshop.getEndDate();
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getWorkshopId() {
    return workshopId;
  }

  public UUID getTrainerId() {
    return trainerId;
  }

  public AllocationStatus getStatus() {
    return status;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }
}
