package com.tms.trainer.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * TrainerCreateRequest as received by {@link TrainerAppService#create} (US-003). Enum-valued fields
 * are kept as strings so an unknown value is reported as a field-level ValidationError
 * (FR-US-003-001/002) rather than an unreadable body. Type-contextual rules (employeeId for
 * INTERNAL, enum membership) are applied by {@link TrainerCreateValidator}.
 *
 * <p>GAP(artifact-SVC-005-api_spec): the TrainerCreateRequest schema is not part of the export.
 * Required fields are those named by FR-US-003-001/002; length limits follow the {@code trainer}
 * table. Commercial attributes (rates, internal incentive applicability) mentioned in the story
 * behaviour have no defined fields and are not accepted yet.
 */
public record TrainerCreateCommand(
    @NotBlank String trainerType,
    @Size(max = 60) String employeeId,
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 200) String location,
    String msaNdaStatus,
    @Size(max = 500) String msaNdaReference,
    Boolean weekendAvailable,
    @Size(max = 100) List<@NotNull UUID> certificationIds) {}
