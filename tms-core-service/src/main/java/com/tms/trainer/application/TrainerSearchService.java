package com.tms.trainer.application;

import com.tms.crosscutting.monitoring.MonitoringComponent;
import com.tms.scheduling.application.AllocationQueryService;
import com.tms.scheduling.application.TrainerAllocationView;
import com.tms.scheduling.domain.AllocationStatus;
import com.tms.trainer.domain.AllocationState;
import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerSearchCriteria;
import com.tms.trainer.domain.TrainerSearchSpecifications;
import com.tms.trainer.domain.TrainerTextSearch;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trainer Application Service: SearchTrainers (FR-US-002-001..FR-US-002-006). */
@Service
@Transactional(readOnly = true)
public class TrainerSearchService {

  public static final String SEARCH_TIMER = "tms.trainer.search.repository";

  private final TrainerRepository trainers;
  private final TrainerTextSearch textSearch;
  private final AllocationQueryService allocations;
  private final MonitoringComponent monitoring;

  public TrainerSearchService(
      TrainerRepository trainers,
      TrainerTextSearch textSearch,
      AllocationQueryService allocations,
      MonitoringComponent monitoring) {
    this.trainers = trainers;
    this.textSearch = textSearch;
    this.allocations = allocations;
    this.monitoring = monitoring;
  }

  /**
   * Structured filters are ANDed (FR-US-002-001); the free-text specification is intersected with
   * them before paging (FR-US-002-002); each returned trainer is enriched with allocation flags
   * from GetTrainerAllocations (FR-US-002-004).
   *
   * <p>TODO(FR-US-002-003): no latency limit is enforced; repository timing is only recorded as
   * the {@value #SEARCH_TIMER} metric until the agreed threshold exists.
   */
  public Page<TrainerDirectoryEntry> searchTrainers(
      TrainerSearchCriteria criteria, int page, int size) {
    Specification<Trainer> spec = buildSpecification(criteria);
    long started = System.nanoTime();
    Page<Trainer> found =
        trainers.findAll(
            spec, PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id"))));
    monitoring.recordTiming(SEARCH_TIMER, Duration.ofNanos(System.nanoTime() - started));

    Set<UUID> ids = found.stream().map(Trainer::getId).collect(Collectors.toSet());
    Map<UUID, List<TrainerAllocationView>> byTrainer =
        allocations.getTrainerAllocations(ids).stream()
            .collect(Collectors.groupingBy(TrainerAllocationView::trainerId));
    return found.map(
        t ->
            TrainerDirectoryEntry.of(
                t, AllocationFlags.from(byTrainer.getOrDefault(t.getId(), List.of()))));
  }

  private Specification<Trainer> buildSpecification(TrainerSearchCriteria criteria) {
    Set<UUID> allocationIds = null;
    boolean exclude = false;
    AllocationState state = criteria.allocationState();
    if (state == AllocationState.FREE) {
      allocationIds = allocations.trainerIdsWithAnyAllocation();
      exclude = true;
    } else if (state == AllocationState.TENTATIVE) {
      allocationIds = allocations.trainerIdsWithStatus(AllocationStatus.TENTATIVE);
    } else if (state == AllocationState.CONFIRMED) {
      allocationIds = allocations.trainerIdsWithStatus(AllocationStatus.CONFIRMED);
    }
    Specification<Trainer> structured =
        TrainerSearchSpecifications.structured(criteria, allocationIds, exclude);
    return textSearch.matching(criteria.text()).map(structured::and).orElse(structured);
  }
}
