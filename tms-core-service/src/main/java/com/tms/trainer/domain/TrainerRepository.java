package com.tms.trainer.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Trainer Repository. SearchTrainers is {@code findAll(Specification, Pageable)}: the structured
 * filters and the free-text term are combined into one specification (see {@link
 * TrainerSearchSpecifications}) so that paging applies to the intersected result (FR-US-002-002).
 */
public interface TrainerRepository
    extends JpaRepository<Trainer, UUID>, JpaSpecificationExecutor<Trainer> {}
