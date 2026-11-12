package com.tms.crosscutting.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Common API error envelope ("API specification - Error handling" in
 * specs/SCAFFOLD-SVC-001/references/SVC-001.md): timestamp, traceId, path, status, error, code,
 * message and optional details[].
 *
 * <p>GAP: the C4 diagram names this type {@code ApiError}; the API contract name {@code
 * ErrorResponse} is used. The full OpenAPI schema body is not included in the export.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    Instant timestamp,
    String traceId,
    String path,
    int status,
    String error,
    String code,
    String message,
    List<ValidationErrorItem> details) {

  public static ErrorResponse of(
      HttpServletRequest request, HttpStatus status, String code, String message) {
    return new ErrorResponse(
        Instant.now(),
        CorrelationIdFilter.currentTraceId(request),
        request.getRequestURI(),
        status.value(),
        status.getReasonPhrase(),
        code,
        message,
        List.of());
  }
}
