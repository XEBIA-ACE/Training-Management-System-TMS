package com.tms.outbox.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatusCode;

/**
 * API model {@code ErrorResponse}: timestamp, status, error, message, path, correlationId and, for
 * validation failures, {@code fieldErrors}.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String correlationId,
    List<FieldValidationError> fieldErrors) {

  public static ErrorResponse of(
      HttpServletRequest request,
      HttpStatusCode status,
      String message,
      List<FieldValidationError> fieldErrors) {
    return new ErrorResponse(
        Instant.now(),
        status.value(),
        reason(status),
        message,
        request.getRequestURI(),
        CorrelationIdFilter.currentCorrelationId(request),
        fieldErrors);
  }

  public static ErrorResponse of(
      HttpServletRequest request, HttpStatusCode status, String message) {
    return of(request, status, message, List.of());
  }

  private static String reason(HttpStatusCode status) {
    org.springframework.http.HttpStatus resolved =
        org.springframework.http.HttpStatus.resolve(status.value());
    return resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
  }
}
