package com.tms.trainer.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** JPA predicates for {@link TrainerRepository#search}. */
final class TrainerSpecifications {

  static final char LIKE_ESCAPE = '\\';

  private TrainerSpecifications() {}

  /** AND of every supplied filter in {@code criteria}; matches all trainers when none is set. */
  static Specification<Trainer> matching(TrainerSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (criteria.status() == null) {
        // FR-US-004-AC2-001/002: INACTIVE trainers only appear on an explicit status filter.
        predicates.add(cb.notEqual(root.get("status"), TrainerStatus.INACTIVE));
      } else if (!criteria.includesAllStatuses()) {
        predicates.add(cb.equal(root.get("status"), criteria.status()));
      }
      if (criteria.availabilityState() != null) {
        predicates.add(cb.equal(root.get("availabilityState"), criteria.availabilityState()));
      }
      if (criteria.locationId() != null) {
        predicates.add(cb.equal(root.get("locationId"), criteria.locationId()));
      }
      if (criteria.weekendAvailable() != null) {
        predicates.add(cb.equal(root.get("weekendAvailable"), criteria.weekendAvailable()));
      }
      if (criteria.certificationId() != null) {
        Subquery<UUID> certified = query.subquery(UUID.class);
        Root<Trainer> t = certified.from(Trainer.class);
        Join<Trainer, UUID> cert = t.join("certificationIds");
        certified
            .select(t.get("id"))
            .where(
                cb.equal(t.get("id"), root.get("id")), cb.equal(cert, criteria.certificationId()));
        predicates.add(cb.exists(certified));
      }
      if (criteria.search() != null) {
        predicates.add(freeText(root, cb, criteria.search()));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  /**
   * FR-US-002-002: case-insensitive "contains" over name, email and employeeId.
   *
   * <p>GAP(FR-US-002-002): the matching algorithm is not specified; an unanchored contains match is
   * the provisional choice (see docs/adr/US-002-trainer-directory-gaps.md).
   */
  private static Predicate freeText(Root<Trainer> root, CriteriaBuilder cb, String term) {
    String pattern = "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
    return cb.or(
        like(cb, root.get("name"), pattern),
        like(cb, root.get("email"), pattern),
        like(cb, root.get("employeeId"), pattern));
  }

  private static Predicate like(CriteriaBuilder cb, Expression<String> field, String pattern) {
    return cb.like(cb.lower(field), pattern, LIKE_ESCAPE);
  }

  static String escapeLike(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (char c : value.toCharArray()) {
      if (c == '%' || c == '_' || c == LIKE_ESCAPE) {
        escaped.append(LIKE_ESCAPE);
      }
      escaped.append(c);
    }
    return escaped.toString();
  }
}
