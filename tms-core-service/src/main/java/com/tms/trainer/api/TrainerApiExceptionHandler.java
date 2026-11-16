package com.tms.trainer.api;

import com.tms.crosscutting.web.CorrelationIdFilter;
import com.tms.crosscutting.web.ErrorResponse;
import com.tms.crosscutting.web.ValidationErrorItem;
import com.tms.trainer.application.TrainerValidationException;
import com.tms.trainer.domain.DuplicateTrainerException;
import com.tms.trainer.domain.TrainerNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps trainer API failures onto the shared {@link ErrorResponse} envelope. */
@RestControllerAdvice(assignableTypes = TrainerController.class)
class TrainerApiExceptionHandler {

  @ExceptionHandler(TrainerNotFoundException.class)
  ResponseEntity<ErrorResponse> notFound(TrainerNotFoundException ex, HttpServletRequest request) {
    return respond(
        ErrorResponse.of(request, HttpStatus.NOT_FOUND, "TRAINER_NOT_FOUND", ex.getMessage()));
  }

  /** FR-US-003-003: distinct codes TRAINER_DUPLICATE_EMPLOYEE_ID / TRAINER_DUPLICATE_EMAIL. */
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
            ex.conflict().errorCode(),
            ex.getMessage(),
            List.of(new ValidationErrorItem(ex.conflict().field(), ex.getMessage()))));
  }

  /** FR-US-003-001/002: every invalid TrainerCreateRequest field as a ValidationError. */
  @ExceptionHandler(TrainerValidationException.class)
  ResponseEntity<ErrorResponse> invalidTrainer(
      TrainerValidationException ex, HttpServletRequest request) {
    return respond(
        validationFailed(
            request,
            ex.violations().stream()
                .map(v -> new ValidationErrorItem(v.field(), v.message()))
                .toList()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErrorResponse> invalidArgument(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    return respond(
        validationFailed(
            request,
            List.of(new ValidationErrorItem(ex.getName(), "Value is not valid for this field."))));
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ErrorResponse> invalidParameters(
      HandlerMethodValidationException ex, HttpServletRequest request) {
    List<ValidationErrorItem> details =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                new ValidationErrorItem(
                                    result.getMethodParameter().getParameterName(),
                                    error.getDefaultMessage())))
            .toList();
    return respond(validationFailed(request, details));
  }

  private static ErrorResponse validationFailed(
      HttpServletRequest request, List<ValidationErrorItem> details) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    return new ErrorResponse(
        Instant.now(),
        CorrelationIdFilter.currentTraceId(request),
        request.getRequestURI(),
        status.value(),
        status.getReasonPhrase(),
        "VALIDATION_FAILED",
        "Request validation failed.",
        details);
  }

  private static ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
    return ResponseEntity.status(body.status()).body(body);
  }
}
