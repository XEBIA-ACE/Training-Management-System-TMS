package com.tms.crosscutting.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

/**
 * Logging and Monitoring Component (FR-US-002-003, FR-US-002-007): emits structured log events
 * (key/values rendered by the structured console format) and records timing metrics.
 */
@Component
public class MonitoringComponent {

  private static final Logger EVENTS = LoggerFactory.getLogger("tms.events");

  private final MeterRegistry meters;

  public MonitoringComponent(MeterRegistry meters) {
    this.meters = meters;
  }

  /** Logs {@code event} with a {@code timestamp} field plus the given fields. */
  public void logEvent(Level level, String event, Map<String, Object> fields) {
    Map<String, Object> all = new LinkedHashMap<>();
    all.put("timestamp", Instant.now().toString());
    all.putAll(fields);
    LoggingEventBuilder builder = EVENTS.atLevel(level);
    all.forEach(builder::addKeyValue);
    builder.log(event);
  }

  public void recordTiming(String metric, Duration elapsed, String... tags) {
    meters.timer(metric, tags).record(elapsed);
  }
}
