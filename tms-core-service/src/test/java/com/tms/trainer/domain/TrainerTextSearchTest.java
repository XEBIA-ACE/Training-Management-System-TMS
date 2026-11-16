package com.tms.trainer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** [US-002-ac-2] baseline free-text semantics (FR-US-002-002 gap: provisional, not approved). */
class TrainerTextSearchTest {

  private final TrainerTextSearch search = new TrainerTextSearch();

  @Test
  @DisplayName("[US-002-ac-2] blank or missing term adds no constraint")
  void us002Ac2_blankTermIsNoConstraint() {
    assertThat(search.matching(null)).isEmpty();
    assertThat(search.matching("   ")).isEmpty();
    assertThat(search.matching("asha")).isPresent();
  }

  @Test
  @DisplayName("[US-002-ac-2] pattern is lower-cased contains with LIKE wildcards escaped")
  void us002Ac2_patternEscapesWildcards() {
    assertThat(TrainerTextSearch.toPattern("  AsHa ")).isEqualTo("%asha%");
    assertThat(TrainerTextSearch.toPattern("50%_x\\")).isEqualTo("%50\\%\\_x\\\\%");
  }
}
