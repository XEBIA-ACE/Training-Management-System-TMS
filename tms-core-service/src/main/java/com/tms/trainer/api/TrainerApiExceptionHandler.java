package com.tms.trainer.api;

import com.tms.crosscutting.web.CorrelationIdFilter;
import com.tms.crosscutting.web.ErrorResponse;
import com.tms.crosscutting.web.ValidationErrorItem;
import com.tms.trainer.domain.DuplicateTrainerException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps trainer API failures onto the shared {@link ErrorResponse} envelope (FR-US-003-002, -004,
 * -011): validation -> 400 VALIDATION_FAILED (as the customer/order APIs); duplicates -> 409
 * TRAINER_DUPLICATE (SVC-001 error handling: 409 for business conflicts; the code is a local
 * choice).
 */
@RestControllerAdvice(assignableTypes = TrainerController.class)
class TrainerApiExceptionHandler {

  @ExceptionHandler(TrainerValidationException.class)
  ResponseEntity<ErrorResponse> invalid(TrainerValidationException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    return respond(
        new ErrorResponse(
            Instant.now(),
            CorrelationIdFilter.currentTraceId(request),
            request.getRequestURI(),
            status.value(),
            status.getReasonPhrase(),
            "VALIDATION_FAILED",
            "Request validation failed.",
            ex.details()));
  }

  @ExceptionHandler(DuplicateTrainerException.class)
  ResponseEntity<ErrorResponse> duplicate(
      DuplicateTrainerException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.CONFLICT;
    return respond(
        new ErrorResponse(
            Instant.now(),
            CorrelationIdFilter.currentTraceId(request),
            request.getRequestURI(),
            status.value(),
            status.getReasonPhrase(),
            "TRAINER_DUPLICATE",
            ex.getMessage(),
            List.of(new ValidationErrorItem(ex.getField(), "already exists"))));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorResponse> unreadable(HttpServletRequest request) {
    return respond(
        ErrorResponse.of(
            request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request body is not valid."));
  }

  private static ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
    return ResponseEntity.status(body.status()).body(body);
  }
}
