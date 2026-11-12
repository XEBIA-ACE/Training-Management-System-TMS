package com.tms.crosscutting.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void propagatesValidRequestIdToMdcAndResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workshops");
    request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            mdcDuringRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY));
          }
        });

    assertThat(mdcDuringRequest.get()).isEqualTo("abc-123");
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
    assertThat(CorrelationIdFilter.currentTraceId(request)).isEqualTo("abc-123");
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void replacesMissingOrUnsafeRequestIdWithGeneratedId() {
    assertThat(CorrelationIdFilter.resolve(null)).hasSize(36);
    assertThat(CorrelationIdFilter.resolve("bad\nvalue")).isNotEqualTo("bad\nvalue").hasSize(36);
    assertThat(CorrelationIdFilter.resolve("x".repeat(129))).hasSize(36);
  }
}
