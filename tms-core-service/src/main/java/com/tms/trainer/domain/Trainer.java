package com.tms.trainer.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * Trainer profile as far as the directory (US-002) needs it.
 *
 * <p>TODO(FR-US-002-005/006): the exact identity/availability field set is not enumerated by the
 * story; {@code availabilityId} is a provisional single availability identifier per trainer.
 */
@Entity
@Table(name = "trainer")
public class Trainer {

  @Id private UUID id;

  @Column(name = "employee_id", nullable = false, length = 60)
  private String employeeId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 254)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TrainerStatus status;

  @Column(length = 120)
  private String location;

  @Column(name = "weekend_available", nullable = false)
  private boolean weekendAvailable;

  @Column(name = "availability_id", nullable = false)
  private UUID availabilityId;

  @ElementCollection
  @CollectionTable(name = "trainer_certification", joinColumns = @JoinColumn(name = "trainer_id"))
  @Column(name = "certification_id", nullable = false)
  @BatchSize(size = 100)
  private Set<UUID> certificationIds = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Trainer() {}

  public Trainer(
      String employeeId,
      String name,
      String email,
      TrainerStatus status,
      String location,
      boolean weekendAvailable,
      Set<UUID> certificationIds,
      Instant now) {
    this.id = UUID.randomUUID();
    this.employeeId = employeeId;
    this.name = name;
    this.email = email;
    this.status = status;
    this.location = location;
    this.weekendAvailable = weekendAvailable;
    this.availabilityId = UUID.randomUUID();
    this.certificationIds = new HashSet<>(certificationIds);
    this.createdAt = now;
    this.updatedAt = now;
  }

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

  public TrainerStatus getStatus() {
    return status;
  }

  public String getLocation() {
    return location;
  }

  public boolean isWeekendAvailable() {
    return weekendAvailable;
  }

  public UUID getAvailabilityId() {
    return availabilityId;
  }

  public Set<UUID> getCertificationIds() {
    return certificationIds;
  }
}
