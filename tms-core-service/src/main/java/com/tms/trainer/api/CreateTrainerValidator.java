package com.tms.trainer.api;

import com.tms.crosscutting.web.ValidationErrorItem;
import com.tms.trainer.application.CreateTrainerCommand;
import com.tms.trainer.domain.TrainerType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Request Validation and Mapping component (FR-US-003-001). Enforces the required fields for the
 * trainer type and maps the payload to a {@link CreateTrainerCommand}.
 *
 * <p>GAP: the story does not list the required fields; they are an implementation choice (name,
 * email, location, weekend availability, certifications, daily rate for all; employee ID for
 * internal; MSA and NDA references for external).
 */
@Component
class CreateTrainerValidator {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  /** ValidateAndMapCreateTrainer. */
  CreateTrainerCommand validateAndMapCreateTrainer(CreateTrainerRequest request) {
    if (request == null) {
      throw new TrainerValidationException(
          List.of(new ValidationErrorItem(null, "Request body is required.")));
    }
    List<ValidationErrorItem> errors = new ArrayList<>();

    TrainerType type = parseType(request.trainerType(), errors);
    String name = required("name", request.name(), 200, errors);
    String email = required("email", request.email(), 254, errors);
    if (email != null) {
      email = email.toLowerCase(Locale.ROOT);
      if (!EMAIL.matcher(email).matches()) {
        errors.add(new ValidationErrorItem("email", "must be a valid email address"));
      }
    }
    String location = required("location", request.location(), 120, errors);
    if (request.weekendAvailable() == null) {
      errors.add(new ValidationErrorItem("weekendAvailable", "is required"));
    }
    List<String> certifications = certifications(request.certifications(), errors);
    BigDecimal rate = request.ratePerDay();
    if (rate == null) {
      errors.add(new ValidationErrorItem("ratePerDay", "is required"));
    } else if (rate.signum() <= 0) {
      errors.add(new ValidationErrorItem("ratePerDay", "must be greater than 0"));
    }

    String employeeId = null;
    String msa = null;
    String nda = null;
    if (type == TrainerType.INTERNAL) {
      employeeId = required("employeeId", request.employeeId(), 64, errors);
    } else if (type == TrainerType.EXTERNAL) {
      msa = required("msaReference", request.msaReference(), 120, errors);
      nda = required("ndaReference", request.ndaReference(), 120, errors);
    }

    if (!errors.isEmpty()) {
      throw new TrainerValidationException(errors);
    }
    return new CreateTrainerCommand(
        type,
        name,
        email,
        employeeId,
        location,
        request.weekendAvailable(),
        certifications,
        rate,
        msa,
        nda);
  }

  private static TrainerType parseType(String value, List<ValidationErrorItem> errors) {
    if (value == null || value.isBlank()) {
      errors.add(new ValidationErrorItem("trainerType", "is required"));
      return null;
    }
    try {
      return TrainerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      errors.add(new ValidationErrorItem("trainerType", "must be INTERNAL or EXTERNAL"));
      return null;
    }
  }

  private static String required(
      String field, String value, int maxLength, List<ValidationErrorItem> errors) {
    if (value == null || value.isBlank()) {
      errors.add(new ValidationErrorItem(field, "is required"));
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      errors.add(new ValidationErrorItem(field, "must be at most " + maxLength + " characters"));
      return null;
    }
    return trimmed;
  }

  private static List<String> certifications(
      List<String> value, List<ValidationErrorItem> errors) {
    if (value == null || value.isEmpty()) {
      errors.add(new ValidationErrorItem("certifications", "at least one is required"));
      return List.of();
    }
    List<String> cleaned = new ArrayList<>();
    for (String c : value) {
      if (c == null || c.isBlank() || c.trim().length() > 200) {
        errors.add(new ValidationErrorItem("certifications", "entries must be 1-200 characters"));
        return List.of();
      }
      cleaned.add(c.trim());
    }
    return cleaned;
  }
}
