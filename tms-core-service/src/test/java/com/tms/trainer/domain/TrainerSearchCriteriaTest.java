package com.tms.trainer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrainerSearchCriteriaTest {

  @Test
  @DisplayName("[US-002-ac-1][US-002-ac-6] blank values are not treated as supplied filters")
  void blankValuesAreIgnored() {
    TrainerSearchCriteria criteria = new TrainerSearchCriteria(" ", null, "", null, null, "  ");
    assertThat(criteria.usedFilterFields()).isEmpty();
    assertThat(criteria.hasFreeText()).isFalse();
  }

  @Test
  @DisplayName("[US-002-ac-6] the criteria summary lists filter field names only")
  void summaryListsFieldNames() {
    TrainerSearchCriteria criteria =
        new TrainerSearchCriteria(
            "ACTIVE", UUID.randomUUID(), "AVAILABLE", UUID.randomUUID(), false, " alice ");
    assertThat(criteria.usedFilterFields())
        .containsExactly(
            "status", "certificationId", "availabilityState", "locationId", "weekendAvailable");
    assertThat(criteria.hasFreeText()).isTrue();
    assertThat(criteria.search()).isEqualTo("alice");
  }

  @Test
  @DisplayName("[US-002-ac-2] LIKE wildcards and the escape character are escaped")
  void escapesLikeWildcards() {
    assertThat(TrainerSpecifications.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
  }
}
