package com.tms.crosscutting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.crosscutting.audit.AuditLogClient;
import com.tms.crosscutting.audit.AuditLogEntry;
import com.tms.crosscutting.audit.AuditLogEntryType;
import com.tms.crosscutting.web.CorrelationIdFilter;
import com.tms.crosscutting.web.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Writes 401/403 security failures as the API {@link ErrorResponse} envelope (codes {@code
 * AUTHENTICATION_FAILED} / {@code ACCESS_DENIED}) while keeping RFC 6750 {@code WWW-Authenticate}
 * headers.
 *
 * <p>Every 401/403 is logged as a security event (logger {@code com.tms.security.events}). A 401 is
 * also written as an UNAUTHORIZED_ACCESS {@link AuditLogEntry} (FR-US-003-007), unless {@code
 * tms.security.audit-authentication-failures=false}; 403s are audited by the application services
 * that make the RBAC decision. Only method, path and trace id are recorded, never tokens.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final AuthenticationEntryPoint bearerEntryPoint =
      new BearerTokenAuthenticationEntryPoint();
  private final AccessDeniedHandler bearerAccessDenied = new BearerTokenAccessDeniedHandler();
  static final String RESOURCE_TYPE = "HttpRequest";
  private static final int MAX_ACTION_LENGTH = 120;
  private static final Logger SECURITY_EVENTS = LoggerFactory.getLogger("com.tms.security.events");

  private final ObjectMapper objectMapper;
  private final AuditLogClient auditLogClient;
  private final boolean auditAuthenticationFailures;

  public SecurityErrorHandler(
      ObjectMapper objectMapper,
      AuditLogClient auditLogClient,
      @Value("${tms.security.audit-authentication-failures:true}")
          boolean auditAuthenticationFailures) {
    this.objectMapper = objectMapper;
    this.auditLogClient = auditLogClient;
    this.auditAuthenticationFailures = auditAuthenticationFailures;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    bearerEntryPoint.commence(request, response, authException);
    String action = action(request);
    SECURITY_EVENTS.warn(
        "security_event=AUTHENTICATION_FAILED action=\"{}\" traceId={}",
        action,
        CorrelationIdFilter.currentTraceId(request));
    if (auditAuthenticationFailures) {
      auditAuthenticationFailure(request, action);
    }
    write(
        request,
        response,
        HttpStatus.UNAUTHORIZED,
        "AUTHENTICATION_FAILED",
        "Authentication is required to access this resource.");
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    bearerAccessDenied.handle(request, response, accessDeniedException);
    SECURITY_EVENTS.warn(
        "security_event=ACCESS_DENIED action=\"{}\" traceId={}",
        action(request),
        CorrelationIdFilter.currentTraceId(request));
    write(
        request,
        response,
        HttpStatus.FORBIDDEN,
        "ACCESS_DENIED",
        "You do not have permission to access this resource.");
  }

  /** A failing audit store must not turn the 401 into a 500; the security event is still logged. */
  private void auditAuthenticationFailure(HttpServletRequest request, String action) {
    try {
      auditLogClient.appendAuditEntry(
          new AuditLogEntry(
              AuditLogEntryType.UNAUTHORIZED_ACCESS,
              AuthContext.ANONYMOUS,
              action,
              RESOURCE_TYPE,
              null,
              CorrelationIdFilter.currentTraceId(request),
              Map.of("reason", "AUTHENTICATION_REQUIRED", "status", 401),
              Instant.now()));
    } catch (RuntimeException ex) {
      SECURITY_EVENTS.error("Could not write the authentication failure audit entry", ex);
    }
  }

  private static String action(HttpServletRequest request) {
    String action = request.getMethod() + " " + request.getRequestURI();
    return action.length() > MAX_ACTION_LENGTH ? action.substring(0, MAX_ACTION_LENGTH) : action;
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(), ErrorResponse.of(request, status, code, message));
  }
}
