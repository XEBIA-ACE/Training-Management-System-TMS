package com.tms.outbox.web;

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
 * Resolves the request correlation id from {@code X-Correlation-ID}, then {@code X-Request-ID}
 * (well-formed values only), otherwise generates a UUID. It is put in MDC key {@code
 * correlationId}, echoed in the {@code X-Correlation-ID} response header and used in {@link
 * ErrorResponse}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-ID";
  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String MDC_KEY = "correlationId";
  static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String correlationId = resolve(request.getHeader(HEADER), request.getHeader(REQUEST_ID_HEADER));
    request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
    response.setHeader(HEADER, correlationId);
    MDC.put(MDC_KEY, correlationId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  static String resolve(String... candidates) {
    for (String candidate : candidates) {
      if (candidate != null && VALID_ID.matcher(candidate).matches()) {
        return candidate;
      }
    }
    return UUID.randomUUID().toString();
  }

  public static String currentCorrelationId(HttpServletRequest request) {
    Object attribute = request.getAttribute(REQUEST_ATTRIBUTE);
    return attribute != null ? attribute.toString() : MDC.get(MDC_KEY);
  }
}
