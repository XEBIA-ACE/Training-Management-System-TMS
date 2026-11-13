package com.tms.outbox.config;

import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * US-001 trainer allocation/deallocation notification settings bound from {@code
 * tms.outbox.trainer-notifications.*}.
 *
 * <p>OPEN QUESTION (US-001): the spec does not define the outbox {@code eventType} values written
 * by application services for trainer allocation/deallocation. The defaults follow the {@code
 * TrainerAllocated} domain event named in specs/SCAFFOLD-SVC-001/references/SVC-001.md plus its
 * symmetric {@code TrainerDeallocated}; override them via {@code
 * TMS_OUTBOX_TRAINER_ALLOCATION_EVENT_TYPES} / {@code TMS_OUTBOX_TRAINER_DEALLOCATION_EVENT_TYPES}.
 *
 * @param allocationEventTypes outbox event types classified as trainer allocation
 * @param deallocationEventTypes outbox event types classified as trainer deallocation
 * @param metricsCacheTtlMs how long the DB-derived backlog gauges reuse one snapshot
 */
@Validated
@ConfigurationProperties("tms.outbox.trainer-notifications")
public record TrainerNotificationProperties(
    @DefaultValue("TrainerAllocated") List<String> allocationEventTypes,
    @DefaultValue("TrainerDeallocated") List<String> deallocationEventTypes,
    @DefaultValue("5000") @Min(0) long metricsCacheTtlMs) {}
