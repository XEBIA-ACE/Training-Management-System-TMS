package com.tms.scheduling.api;

import com.tms.crosscutting.web.ErrorResponse;
import com.tms.scheduling.domain.AllocationConflictException;
import com.tms.scheduling.domain.TrainerUnavailableException;
import com.tms.scheduling.domain.WorkshopNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps AllocateTrainer failures onto the shared error envelope (codes are provisional). */
@RestControllerAdvice(assignableTypes = WorkshopAllocationController.class)
class AllocationApiExceptionHandler {

  @ExceptionHandler(WorkshopNotFoundException.class)
  ResponseEntity<ErrorResponse> workshopNotFound(
      WorkshopNotFoundException ex, HttpServletRequest request) {
    return respond(request, HttpStatus.NOT_FOUND, "WORKSHOP_NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(TrainerUnavailableException.class)
  ResponseEntity<ErrorResponse> unavailable(
      TrainerUnavailableException ex, HttpServletRequest request) {
    return respond(request, HttpStatus.CONFLICT, "TRAINER_UNAVAILABLE", ex.getMessage());
  }

  @ExceptionHandler(AllocationConflictException.class)
  ResponseEntity<ErrorResponse> conflict(
      AllocationConflictException ex, HttpServletRequest request) {
    return respond(request, HttpStatus.CONFLICT, "ALLOCATION_CONFLICT", ex.getMessage());
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ErrorResponse> invalid(HttpServletRequest request) {
    return respond(
        request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.");
  }

  private static ResponseEntity<ErrorResponse> respond(
      HttpServletRequest request, HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(ErrorResponse.of(request, status, code, message));
  }
}
