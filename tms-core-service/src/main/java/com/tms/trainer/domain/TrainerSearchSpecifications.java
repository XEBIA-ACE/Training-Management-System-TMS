package com.tms.trainer.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Structured-filter part of Trainer Repository SearchTrainers (FR-US-001/FR-US-002-001): every
 * supplied filter is ANDed.
 */
public final class TrainerSearchSpecifications {

  private TrainerSearchSpecifications() {}

  /**
   * Builds the AND of all supplied structured filters.
   *
   * @param allocationFilterIds ids resolved from the allocation-state filter by the application
   *     layer; {@code null} when no allocation-state filter was supplied
   * @param allocationFilterExcludes true when the ids are to be excluded (state FREE)
   */
  public static Specification<Trainer> structured(
      TrainerSearchCriteria criteria,
      Set<UUID> allocationFilterIds,
      boolean allocationFilterExcludes) {
    List<Specification<Trainer>> parts = new ArrayList<>();
    if (criteria.status() != null) {
      parts.add((root, query, cb) -> cb.equal(root.get("status"), criteria.status()));
    }
    if (criteria.certificationIds() != null) {
      for (UUID certificationId : criteria.certificationIds()) {
        parts.add(
            (root, query, cb) ->
                cb.isMember(certificationId, root.<Set<UUID>>get("certificationIds")));
      }
    }
    if (criteria.location() != null && !criteria.location().isBlank()) {
      String location = criteria.location().trim().toLowerCase();
      parts.add((root, query, cb) -> cb.equal(cb.lower(root.get("location")), location));
    }
    if (criteria.weekendAvailable() != null) {
      parts.add(
          (root, query, cb) ->
              cb.equal(root.get("weekendAvailable"), criteria.weekendAvailable()));
    }
    if (allocationFilterIds != null) {
      if (allocationFilterExcludes) {
        if (!allocationFilterIds.isEmpty()) {
          parts.add((root, query, cb) -> cb.not(root.get("id").in(allocationFilterIds)));
        }
      } else if (allocationFilterIds.isEmpty()) {
        parts.add((root, query, cb) -> cb.disjunction());
      } else {
        parts.add((root, query, cb) -> root.get("id").in(allocationFilterIds));
      }
    }
    return Specification.allOf(parts);
  }
}
