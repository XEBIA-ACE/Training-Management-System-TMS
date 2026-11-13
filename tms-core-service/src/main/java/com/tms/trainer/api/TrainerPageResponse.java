package com.tms.trainer.api;

import com.tms.trainer.application.TrainerDirectoryEntry;
import java.util.List;
import org.springframework.data.domain.Page;

/** One page of GET /api/v1/trainers; page metadata mirrors the customer list response. */
public record TrainerPageResponse(
    List<TrainerSummaryResponse> content, int page, int size, long totalElements, int totalPages) {

  static TrainerPageResponse from(Page<TrainerDirectoryEntry> page) {
    return new TrainerPageResponse(
        page.getContent().stream().map(TrainerSummaryResponse::from).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
