package com.tms.trainer.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * Trainer aggregate as read by the trainer directory (US-002).
 *
 * <p>GAP(FR-US-002-001): the allowed values of {@code status} and {@code availabilityState} are not
 * defined by the story, so they are stored and filtered as opaque strings (exact match).
 */
@Entity
@Table(name = "trainer")
public class Trainer {

  @Id private UUID id;

  @Column(name = "employee_id", length = 60)
  private String employeeId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(nullable = false, length = 40)
  private String status;

  @Column(name = "availability_state", length = 40)
  private String availabilityState;

  @Column(name = "location_id")
  private UUID locationId;

  @Column(name = "weekend_available", nullable = false)
  private boolean weekendAvailable;

  /** Batch-loaded so a page of trainers needs one extra query, not one per trainer. */
  @ElementCollection
  @CollectionTable(name = "trainer_certification", joinColumns = @JoinColumn(name = "trainer_id"))
  @Column(name = "certification_id", nullable = false)
  @BatchSize(size = 100)
  private Set<UUID> certificationIds = new LinkedHashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Trainer() {}

  public UUID getId() {
    return id;
  }

  public String getEmployeeId() {
    return employeeId;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getStatus() {
    return status;
  }

  public String getAvailabilityState() {
    return availabilityState;
  }

  public UUID getLocationId() {
    return locationId;
  }

  public boolean isWeekendAvailable() {
    return weekendAvailable;
  }

  public Set<UUID> getCertificationIds() {
    return Collections.unmodifiableSet(certificationIds);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
