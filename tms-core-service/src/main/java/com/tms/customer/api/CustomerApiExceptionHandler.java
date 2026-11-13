package com.tms.customer.api;

import com.tms.crosscutting.web.CorrelationIdFilter;
import com.tms.crosscutting.web.ErrorResponse;
import com.tms.crosscutting.web.ValidationErrorItem;
import com.tms.customer.domain.CustomerNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps customer API failures onto the shared {@link ErrorResponse} envelope. */
@RestControllerAdvice(assignableTypes = CustomerController.class)
class CustomerApiExceptionHandler {

  @ExceptionHandler(CustomerNotFoundException.class)
  ResponseEntity<ErrorResponse> notFound(CustomerNotFoundException ex, HttpServletRequest request) {
    return respond(
        ErrorResponse.of(request, HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErrorResponse> invalidId(HttpServletRequest request) {
    return respond(
        ErrorResponse.of(
            request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Customer ID is not valid."));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorResponse> unreadable(HttpServletRequest request) {
    return respond(
        ErrorResponse.of(
            request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request body is not valid."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> invalid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ValidationErrorItem> details =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                error ->
                    new ValidationErrorItem(
                        error instanceof FieldError field
                            ? field.getField()
                            : error.getObjectName(),
                        error.getDefaultMessage()))
            .toList();
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
            details));
  }

  private static ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
    return ResponseEntity.status(body.status()).body(body);
  }
}
