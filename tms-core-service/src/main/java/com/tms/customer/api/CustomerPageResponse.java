package com.tms.customer.api;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * One page of {@code GET /api/v1/customers} (FR-US-001-AC2, T002). Shape follows the page/size/
 * totalElements fields of the C4 {@code DashboardSessionPageResponse}.
 *
 * <p>GAP(artifact-SVC-001-api_spec): the {@code CustomerPage}/{@code PageMetadata} schemas have no
 * body in the export; {@code content} and {@code totalPages} are an implementation choice.
 */
public record CustomerPageResponse(
    List<CustomerResponse> content, int page, int size, long totalElements, int totalPages) {

  static CustomerPageResponse from(Page<CustomerResponse> page) {
    return new CustomerPageResponse(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
