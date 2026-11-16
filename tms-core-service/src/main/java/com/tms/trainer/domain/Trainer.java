package com.tms.trainer.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Trainer aggregate (US-003). New trainers are always {@link TrainerStatus#ACTIVE}
 * (FR-US-003-007). GAP: the profile field set is an implementation choice.
 */
@Entity
@Table(name = "trainer")
public class Trainer {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "trainer_type", nullable = false, updatable = false)
  private TrainerType trainerType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TrainerStatus status;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "employee_id", length = 64)
  private String employeeId;

  @Column(nullable = false, length = 120)
  private String location;

  @Column(name = "time_zone", length = 64)
  private String timeZone;

  @Column(name = "weekend_available", nullable = false)
  private boolean weekendAvailable;

  @Column(name = "rate_per_day", nullable = false)
  private BigDecimal ratePerDay;

  @Column(name = "msa_reference", length = 120)
  private String msaReference;

  @Column(name = "nda_reference", length = 120)
  private String ndaReference;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "trainer_certification", joinColumns = @JoinColumn(name = "trainer_id"))
  @Column(name = "certification", nullable = false, length = 200)
  private List<String> certifications;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Trainer() {}

  public Trainer(
      TrainerType trainerType,
      String name,
      String email,
      String employeeId,
      String location,
      String timeZone,
      boolean weekendAvailable,
      BigDecimal ratePerDay,
      String msaReference,
      String ndaReference,
      List<String> certifications,
      Instant now) {
    this.id = UUID.randomUUID();
    this.trainerType = trainerType;
    this.status = TrainerStatus.ACTIVE;
    this.name = name;
    this.email = email;
    this.employeeId = employeeId;
    this.location = location;
    this.timeZone = timeZone;
    this.weekendAvailable = weekendAvailable;
    this.ratePerDay = ratePerDay;
    this.msaReference = msaReference;
    this.ndaReference = ndaReference;
    this.certifications = List.copyOf(certifications);
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TrainerType getTrainerType() {
    return trainerType;
  }

  public TrainerStatus getStatus() {
    return status;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getEmployeeId() {
    return employeeId;
  }

  public String getLocation() {
    return location;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public boolean isWeekendAvailable() {
    return weekendAvailable;
  }

  public BigDecimal getRatePerDay() {
    return ratePerDay;
  }

  public String getMsaReference() {
    return msaReference;
  }

  public String getNdaReference() {
    return ndaReference;
  }

  public List<String> getCertifications() {
    return certifications;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
