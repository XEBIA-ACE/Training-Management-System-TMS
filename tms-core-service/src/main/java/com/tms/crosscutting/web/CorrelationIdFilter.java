package com.tms.crosscutting.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation id to every request: the client's {@code X-Request-Id} when well-formed,
 * otherwise a new UUID. It is exposed as MDC key {@code traceId} (structured logs), echoed in the
 * {@code X-Request-Id} response header and used as {@link ErrorResponse#traceId()}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Request-Id";
  public static final String MDC_KEY = "traceId";
  static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".traceId";

  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = resolve(request.getHeader(HEADER));
    request.setAttribute(REQUEST_ATTRIBUTE, traceId);
    response.setHeader(HEADER, traceId);
    MDC.put(MDC_KEY, traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  static String resolve(String candidate) {
    return candidate != null && VALID_ID.matcher(candidate).matches()
        ? candidate
        : UUID.randomUUID().toString();
  }

  public static String currentTraceId(HttpServletRequest request) {
    Object attribute = request.getAttribute(REQUEST_ATTRIBUTE);
    return attribute != null ? attribute.toString() : MDC.get(MDC_KEY);
  }
}
