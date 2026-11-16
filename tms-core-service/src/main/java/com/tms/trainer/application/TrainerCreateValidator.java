package com.tms.trainer.application;

import com.tms.trainer.application.TrainerValidationException.FieldViolation;
import com.tms.trainer.domain.MsaNdaStatus;
import com.tms.trainer.domain.TrainerType;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ValidationEngine for TrainerCreateRequest: Bean Validation constraints of {@link
 * TrainerCreateCommand} plus the trainerType-contextual rules.
 *
 * <ul>
 *   <li>FR-US-003-001 (US-003-ac-1): INTERNAL requires a non-empty employeeId, a well-formed email,
 *       name and location.
 *   <li>FR-US-003-002 (US-003-ac-2): EXTERNAL requires a well-formed email, name and location;
 *       msaNdaStatus, when supplied, must be a {@link MsaNdaStatus} value.
 * </ul>
 *
 * Every invalid field is reported (not only the first), ordered by field name.
 */
@Component
public class TrainerCreateValidator {

  private final Validator validator;

  public TrainerCreateValidator(Validator validator) {
    this.validator = validator;
  }

  public List<FieldViolation> validate(TrainerCreateCommand command) {
    List<FieldViolation> violations = new ArrayList<>();
    validator
        .validate(command)
        .forEach(
            v ->
                violations.add(new FieldViolation(v.getPropertyPath().toString(), v.getMessage())));

    String type = command.trainerType();
    if (type != null && !type.isBlank() && TrainerType.parse(type).isEmpty()) {
      violations.add(new FieldViolation("trainerType", mustBeOneOf(TrainerType.values())));
    }
    if (TrainerType.INTERNAL.name().equals(type)
        && (command.employeeId() == null || command.employeeId().isBlank())) {
      violations.add(
          new FieldViolation("employeeId", "must not be blank for trainerType INTERNAL"));
    }
    if (command.msaNdaStatus() != null && MsaNdaStatus.parse(command.msaNdaStatus()).isEmpty()) {
      violations.add(new FieldViolation("msaNdaStatus", mustBeOneOf(MsaNdaStatus.values())));
    }
    violations.sort(
        Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message));
    return violations;
  }

  private static String mustBeOneOf(Enum<?>[] values) {
    return "must be one of " + Arrays.toString(values);
  }
}
