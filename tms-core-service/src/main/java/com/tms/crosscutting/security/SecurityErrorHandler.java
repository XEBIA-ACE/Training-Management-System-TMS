package com.tms.crosscutting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.crosscutting.web.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final AuthenticationEntryPoint bearerEntryPoint =
      new BearerTokenAuthenticationEntryPoint();
  private final AccessDeniedHandler bearerAccessDenied = new BearerTokenAccessDeniedHandler();
  private final ObjectMapper objectMapper;

  public SecurityErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    bearerEntryPoint.commence(request, response, authException);
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
    write(
        request,
        response,
        HttpStatus.FORBIDDEN,
        "ACCESS_DENIED",
        "You do not have permission to access this resource.");
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
