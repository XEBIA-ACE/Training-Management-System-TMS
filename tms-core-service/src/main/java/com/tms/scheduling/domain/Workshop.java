package com.tms.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Minimal workshop record (US-002): only what trainer allocation needs (ID and date range).
 * Requirement/customer links and status lifecycle are left to later stories.
 */
@Entity
@Table(name = "workshop")
public class Workshop {

  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Workshop() {}

  public Workshop(String name, LocalDate startDate, LocalDate endDate, Instant now) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }
}
