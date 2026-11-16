package com.tms.trainer.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filters of {@code GET /api/trainers} (FR-US-002-001/002). A {@code null} field means "not
 * supplied"; every supplied field is AND-combined. Blank strings are normalised to {@code null}.
 */
public record TrainerSearchCriteria(
    String status,
    UUID certificationId,
    String availabilityState,
    UUID locationId,
    Boolean weekendAvailable,
    String search) {

  public TrainerSearchCriteria {
    status = normalise(status);
    availabilityState = normalise(availabilityState);
    search = normalise(search);
  }

  public boolean hasFreeText() {
    return search != null;
  }

  /** Names of the supplied structured filters (excludes {@code search}). */
  public List<String> usedFilterFields() {
    List<String> fields = new ArrayList<>();
    if (status != null) {
      fields.add("status");
    }
    if (certificationId != null) {
      fields.add("certificationId");
    }
    if (availabilityState != null) {
      fields.add("availabilityState");
    }
    if (locationId != null) {
      fields.add("locationId");
    }
    if (weekendAvailable != null) {
      fields.add("weekendAvailable");
    }
    return List.copyOf(fields);
  }

  private static String normalise(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
