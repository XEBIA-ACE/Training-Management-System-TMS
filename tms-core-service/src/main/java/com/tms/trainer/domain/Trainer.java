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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * Trainer aggregate as read by the trainer directory (US-002) and created by {@code POST
 * /api/v1/trainers} (US-003).
 *
 * <p>GAP(FR-US-002-001): the allowed values of {@code status} and {@code availabilityState} are not
 * defined by the story, so they are stored and filtered as opaque strings (exact match).
 */
@Entity
@Table(name = "trainer")
public class Trainer {

  /** FR-US-003-005: status of every newly created trainer. */
  public static final String STATUS_ACTIVE = "ACTIVE";

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

  /** {@code null} for trainers that pre-date US-003. */
  @Enumerated(EnumType.STRING)
  @Column(name = "trainer_type", length = 20)
  private TrainerType trainerType;

  /** Free-text location of the TrainerCreateRequest (FR-US-003-004). */
  @Column(length = 200)
  private String location;

  /** IANA time zone id derived from {@link #location} (FR-US-003-004). */
  @Column(name = "time_zone", length = 64)
  private String timeZone;

  @Enumerated(EnumType.STRING)
  @Column(name = "msa_nda_status", length = 40)
  private MsaNdaStatus msaNdaStatus;

  /** Link or identifier of the MSA/NDA document, never the document contents. */
  @Column(name = "msa_nda_reference", length = 500)
  private String msaNdaReference;

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

  /** FR-US-003-005: a new trainer, always created with status {@value #STATUS_ACTIVE}. */
  public static Trainer create(
      UUID id,
      TrainerType trainerType,
      String employeeId,
      String name,
      String email,
      String location,
      String timeZone,
      MsaNdaStatus msaNdaStatus,
      String msaNdaReference,
      boolean weekendAvailable,
      Collection<UUID> certificationIds,
      Instant now) {
    Trainer trainer = new Trainer();
    trainer.id = id;
    trainer.trainerType = trainerType;
    trainer.employeeId = employeeId;
    trainer.name = name;
    trainer.email = email;
    trainer.location = location;
    trainer.timeZone = timeZone;
    trainer.msaNdaStatus = msaNdaStatus;
    trainer.msaNdaReference = msaNdaReference;
    trainer.weekendAvailable = weekendAvailable;
    trainer.certificationIds.addAll(certificationIds);
    trainer.status = STATUS_ACTIVE;
    trainer.createdAt = now;
    trainer.updatedAt = now;
    return trainer;
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

  public TrainerType getTrainerType() {
    return trainerType;
  }

  public String getLocation() {
    return location;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public MsaNdaStatus getMsaNdaStatus() {
    return msaNdaStatus;
  }

  public String getMsaNdaReference() {
    return msaNdaReference;
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
