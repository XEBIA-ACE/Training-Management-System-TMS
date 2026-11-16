package com.tms.trainer.application;

import com.tms.scheduling.application.TrainerAllocationView;
import com.tms.scheduling.domain.AllocationStatus;
import java.util.List;

/**
 * Per-trainer allocation indicators (FR-US-002-004).
 *
 * <p>TODO(FR-US-002-004, gap "how overlapping or conflicting states are represented"): flags are
 * independent booleans, so a trainer with both a tentative and a confirmed allocation has both set
 * (and {@code free} false). This is a straightforward mapping, not a product decision.
 */
public record AllocationFlags(boolean free, boolean tentative, boolean confirmed) {

  public static AllocationFlags from(List<TrainerAllocationView> allocations) {
    boolean tentative = has(allocations, AllocationStatus.TENTATIVE);
    boolean confirmed = has(allocations, AllocationStatus.CONFIRMED);
    return new AllocationFlags(!tentative && !confirmed, tentative, confirmed);
  }

  private static boolean has(List<TrainerAllocationView> allocations, AllocationStatus status) {
    return allocations.stream().anyMatch(a -> a.status() == status);
  }
}
