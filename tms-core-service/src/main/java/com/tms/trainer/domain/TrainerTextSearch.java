package com.tms.trainer.domain;

import java.util.Locale;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Trainer Domain Service for the free-text term (FR-US-002-002): matches name, email and employee
 * identifier. The returned specification is ANDed with the structured filters before paging.
 *
 * <p>TODO(FR-US-002-002, gap "exact matching behavior"): the baseline is a case-insensitive,
 * unanchored contains over the whole trimmed term (no tokenization), using portable JPA criteria
 * rather than a vendor feature. It is not a product-approved decision; change {@link
 * #toPattern(String)} to alter it.
 */
@Component
public class TrainerTextSearch {

  public Optional<Specification<Trainer>> matching(String term) {
    if (term == null || term.isBlank()) {
      return Optional.empty();
    }
    String pattern = toPattern(term);
    return Optional.of(
        (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern, '\\'),
                cb.like(cb.lower(root.get("email")), pattern, '\\'),
                cb.like(cb.lower(root.get("employeeId")), pattern, '\\')));
  }

  /** Lower-cased contains pattern with LIKE wildcards in the term escaped. */
  static String toPattern(String term) {
    String escaped =
        term.trim()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
