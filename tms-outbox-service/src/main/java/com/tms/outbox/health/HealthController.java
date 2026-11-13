package com.tms.outbox.health;

import com.tms.outbox.api.dto.HealthResponse;
import com.tms.outbox.api.dto.HealthResponse.ComponentStatus;
import com.tms.outbox.api.dto.HealthResponse.Components;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C3 Health Monitor: {@code GET /health}, {@code /health/ready}, {@code /health/live} as {@code
 * HealthResponse}, delegating to Actuator (overall health and the {@code readiness} / {@code
 * liveness} probe groups). 200 when UP, 503 otherwise.
 */
@RestController
public class HealthController {

  private final HealthEndpoint healthEndpoint;

  public HealthController(HealthEndpoint healthEndpoint) {
    this.healthEndpoint = healthEndpoint;
  }

  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return toResponse(healthEndpoint.health());
  }

  @GetMapping("/health/ready")
  public ResponseEntity<HealthResponse> ready() {
    return toResponse(healthEndpoint.healthForPath("readiness"));
  }

  @GetMapping("/health/live")
  public ResponseEntity<HealthResponse> live() {
    return toResponse(healthEndpoint.healthForPath("liveness"));
  }

  private static ResponseEntity<HealthResponse> toResponse(HealthComponent health) {
    boolean up = health != null && Status.UP.equals(health.getStatus());
    Map<String, HealthComponent> parts =
        health instanceof CompositeHealth composite ? composite.getComponents() : Map.of();
    HealthResponse body =
        new HealthResponse(
            up ? HealthResponse.UP : HealthResponse.DOWN,
            Instant.now(),
            new Components(
                status(parts.get("db")),
                status(parts.get("processor")),
                status(parts.get("messageBroker")),
                status(parts.get("outboxPublishing"))));
    return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  private static ComponentStatus status(HealthComponent component) {
    return component != null ? new ComponentStatus(component.getStatus().getCode()) : null;
  }
}
