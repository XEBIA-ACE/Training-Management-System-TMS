package com.tms.outbox.health;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /metrics}: Prometheus text exposition of the Micrometer registry. */
@RestController
public class MetricsController {

  static final String PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

  private final PrometheusMeterRegistry registry;

  public MetricsController(PrometheusMeterRegistry registry) {
    this.registry = registry;
  }

  @GetMapping("/metrics")
  public ResponseEntity<String> metrics() {
    return ResponseEntity.ok()
        .header("Content-Type", PROMETHEUS_CONTENT_TYPE)
        .body(registry.scrape());
  }
}
