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

  @Column(length = 20)
  private String type;

  @Column(length = 64)
  private String timezone;

  @Column(name = "msa_nda_status", length = 20)
  private String msaNdaStatus;

  @Column(name = "weekend_available", nullable = false)
  private boolean weekendAvailable;

  /** Batch-loaded so a page of trainers needs one extra query, not one per trainer. */
  @ElementCollection
  @CollectionTable(name = "trainer_certification", joinColumns = @JoinColumn(name = "trainer_id"))
  @Column(name = "certification_id", nullable = false)
  @BatchSize(size = 100)
  private Set<UUID> certificationIds = new LinkedHashSet<>();

  @Column(name = "status_changed_at")
  private Instant statusChangedAt;

  @Column(name = "status_changed_by", length = 200)
  private String statusChangedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Trainer() {}

  /** FR-US-003-005: new trainers are ACTIVE without any further status call. */
  public static Trainer create(
      TrainerType type,
      String employeeId,
      String name,
      String email,
      UUID locationId,
      String timezone,
      boolean weekendAvailable,
      MsaNdaStatus msaNdaStatus,
      Instant now) {
    Trainer t = new Trainer();
    t.id = UUID.randomUUID();
    t.type = type.name();
    t.employeeId = employeeId;
    t.name = name;
    t.email = email;
    t.status = "ACTIVE";
    t.locationId = locationId;
    t.timezone = timezone;
    t.weekendAvailable = weekendAvailable;
    t.msaNdaStatus = msaNdaStatus != null ? msaNdaStatus.name() : null;
    t.createdAt = now;
    t.updatedAt = now;
    return t;
  }

  /**
   * FR-US-003-004: a changed location recalculates the timezone. GAP: when the new location has no
   * known timezone the previous timezone is kept (behavior undefined by the story).
   */
  public void update(String name, UUID newLocationId, String derivedTimezone, Instant now) {
    if (name != null) {
      this.name = name;
    }
    if (newLocationId != null && !newLocationId.equals(this.locationId)) {
      this.locationId = newLocationId;
      if (derivedTimezone != null) {
        this.timezone = derivedTimezone;
      }
    }
    this.updatedAt = now;
  }

  /**
   * FR-US-004-AC1-002: writes only {@code status} and its metadata (not {@code updatedAt}, which
   * tracks profile edits).
   */
  public void changeStatus(String newStatus, String actor, Instant now) {
    this.status = newStatus;
    this.statusChangedAt = now;
    this.statusChangedBy = actor;
  }

  /** FR-US-004-AC3-001: an INACTIVE trainer must not receive new allocations. */
  public boolean isInactive() {
    return TrainerStatus.INACTIVE.equals(status);
  }

  public Instant getStatusChangedAt() {
    return statusChangedAt;
  }

  public String getStatusChangedBy() {
    return statusChangedBy;
  }

  public String getType() {
    return type;
  }

  public String getTimezone() {
    return timezone;
  }

  public String getMsaNdaStatus() {
    return msaNdaStatus;
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
