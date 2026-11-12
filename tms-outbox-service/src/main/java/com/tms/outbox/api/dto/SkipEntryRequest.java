package com.tms.outbox.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** API model {@code SkipEntryRequest}. */
public record SkipEntryRequest(@NotBlank @Size(min = 1, max = 500) String reason) {}
