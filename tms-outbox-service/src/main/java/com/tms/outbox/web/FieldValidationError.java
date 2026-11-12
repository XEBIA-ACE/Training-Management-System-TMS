package com.tms.outbox.web;

/** Entry of {@link ErrorResponse#fieldErrors()}: field name, rejected value and message. */
public record FieldValidationError(String field, Object rejectedValue, String message) {}
