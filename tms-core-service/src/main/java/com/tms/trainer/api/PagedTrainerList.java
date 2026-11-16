package com.tms.trainer.api;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Response of {@code GET /api/trainers}: one page of trainers matching every supplied filter.
 *
 * <p>GAP(artifact-SVC-008-api_spec): only {@code items} is named by the story; the paging fields
 * follow {@code CustomerPageResponse}.
 */
public record PagedTrainerList(
    List<TrainerResponse> items, int page, int size, long totalElements, int totalPages) {

  static PagedTrainerList from(Page<TrainerResponse> page) {
    return new PagedTrainerList(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
