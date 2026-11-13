package com.tms.trainer.api;

import com.tms.crosscutting.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps trainer directory request errors onto the shared {@link ErrorResponse} envelope. */
@RestControllerAdvice(assignableTypes = TrainerController.class)
class TrainerApiExceptionHandler {

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErrorResponse> invalidArgument(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    return respond(request, "Parameter '" + ex.getName() + "' is not valid.");
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ErrorResponse> invalidParameters(HttpServletRequest request) {
    return respond(request, "Request validation failed.");
  }

  private static ResponseEntity<ErrorResponse> respond(HttpServletRequest request, String message) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(status)
        .body(ErrorResponse.of(request, status, "VALIDATION_FAILED", message));
  }
}
