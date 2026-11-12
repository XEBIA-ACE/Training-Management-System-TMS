package com.tms.outbox.web;

import com.tms.outbox.api.ProcessorStateConflictException;
import com.tms.outbox.domain.InvalidOutboxStateException;
import com.tms.outbox.domain.OutboxEntryNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps exceptions to the {@link ErrorResponse} envelope ("API specification - Error handling"). */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(OutboxEntryNotFoundException.class)
  ResponseEntity<ErrorResponse> notFound(OutboxEntryNotFoundException ex, HttpServletRequest req) {
    return respond(req, HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
  }

  /** 400 per the OpenAPI responses of retry/skip ("Entry is not in a retryable state"). */
  @ExceptionHandler(InvalidOutboxStateException.class)
  ResponseEntity<ErrorResponse> invalidState(
      InvalidOutboxStateException ex, HttpServletRequest req) {
    return respond(req, HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
  }

  @ExceptionHandler(ProcessorStateConflictException.class)
  ResponseEntity<ErrorResponse> conflict(
      ProcessorStateConflictException ex, HttpServletRequest req) {
    return respond(req, HttpStatus.CONFLICT, ex.getMessage(), List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
    return respond(
        req,
        HttpStatus.FORBIDDEN,
        "You do not have the required scope to access this resource.",
        List.of());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ErrorResponse> constraintViolation(
      ConstraintViolationException ex, HttpServletRequest req) {
    List<FieldValidationError> errors =
        ex.getConstraintViolations().stream()
            .map(
                v ->
                    new FieldValidationError(
                        v.getPropertyPath().toString(), v.getInvalidValue(), v.getMessage()))
            .toList();
    return respond(req, HttpStatus.BAD_REQUEST, "Validation failed", errors);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
    log.error("Unhandled error on {}", req.getRequestURI(), ex);
    return respond(
        req, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", List.of());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldValidationError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                e ->
                    new FieldValidationError(
                        e.getField(), e.getRejectedValue(), e.getDefaultMessage()))
            .toList();
    return body(request, headers, status, "Validation failed", errors);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldValidationError> errors = new ArrayList<>();
    ex.getParameterValidationResults()
        .forEach(
            result ->
                result
                    .getResolvableErrors()
                    .forEach(
                        error ->
                            errors.add(
                                new FieldValidationError(
                                    result.getMethodParameter().getParameterName(),
                                    result.getArgument(),
                                    error.getDefaultMessage()))));
    return body(request, headers, status, "Validation failed", errors);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    String message =
        ex instanceof org.springframework.web.ErrorResponse springError
                && springError.getBody().getDetail() != null
            ? springError.getBody().getDetail()
            : ex.getClass().getSimpleName();
    return body(request, headers, statusCode, message, List.of());
  }

  private ResponseEntity<Object> body(
      WebRequest request,
      HttpHeaders headers,
      HttpStatusCode status,
      String message,
      List<FieldValidationError> errors) {
    HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
    return ResponseEntity.status(status)
        .headers(headers)
        .body(ErrorResponse.of(servletRequest, status, message, errors));
  }

  private static ResponseEntity<ErrorResponse> respond(
      HttpServletRequest request,
      HttpStatus status,
      String message,
      List<FieldValidationError> errors) {
    return ResponseEntity.status(status).body(ErrorResponse.of(request, status, message, errors));
  }
}
