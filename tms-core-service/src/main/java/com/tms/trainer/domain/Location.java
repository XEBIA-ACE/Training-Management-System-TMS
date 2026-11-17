package com.tms.trainer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Read-only location lookup used to derive {@link Trainer#getTimezone()} (FR-US-003-004).
 *
 * <p>GAP(FR-US-003-004): stands in for CustomerLocation, which does not exist in this repository.
 */
@Entity
@Table(name = "location")
public class Location {

  @Id private UUID id;

  @Column(length = 64)
  private String timezone;

  protected Location() {}

  public UUID getId() {
    return id;
  }

  public String getTimezone() {
    return timezone;
  }
}
