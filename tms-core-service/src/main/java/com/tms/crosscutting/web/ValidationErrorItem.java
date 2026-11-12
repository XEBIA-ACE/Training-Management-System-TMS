package com.tms.crosscutting.web;

/**
 * Field-level validation issue inside {@link ErrorResponse#details()}.
 *
 * <p>GAP (specs/SCAFFOLD-SVC-001/references/SVC-001.md, "API specification - Data models"
 * ValidationErrorItem): the schema body is not included in the export; fields follow the "Error
 * handling" section ("details listing each offending field and message").
 */
public record ValidationErrorItem(String field, String message) {}
