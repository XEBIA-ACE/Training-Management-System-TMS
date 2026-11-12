package com.tms.outbox.api.dto;

import java.util.List;

/** API model {@code PagedOutboxEntriesResponse}. */
public record PagedOutboxEntriesResponse(
    List<OutboxEntryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {}
