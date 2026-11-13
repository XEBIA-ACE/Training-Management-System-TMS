package com.tms.trainer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.tms.scheduling.application.TrainerAllocationView;
import com.tms.scheduling.domain.AllocationStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** [US-002-ac-4] flag derivation (overlap representation is provisional, FR-US-002-004). */
class AllocationFlagsTest {

  private static TrainerAllocationView view(AllocationStatus status) {
    return new TrainerAllocationView(UUID.randomUUID(), UUID.randomUUID(), status);
  }

  @Test
  @DisplayName("[US-002-ac-4] no allocations means free")
  void us002Ac4_noAllocationsIsFree() {
    assertThat(AllocationFlags.from(List.of())).isEqualTo(new AllocationFlags(true, false, false));
  }

  @Test
  @DisplayName("[US-002-ac-4] tentative, confirmed and mixed allocations set independent flags")
  void us002Ac4_statesSetIndependentFlags() {
    assertThat(AllocationFlags.from(List.of(view(AllocationStatus.TENTATIVE))))
        .isEqualTo(new AllocationFlags(false, true, false));
    assertThat(AllocationFlags.from(List.of(view(AllocationStatus.CONFIRMED))))
        .isEqualTo(new AllocationFlags(false, false, true));
    assertThat(
            AllocationFlags.from(
                List.of(view(AllocationStatus.CONFIRMED), view(AllocationStatus.TENTATIVE))))
        .isEqualTo(new AllocationFlags(false, true, true));
  }
}
