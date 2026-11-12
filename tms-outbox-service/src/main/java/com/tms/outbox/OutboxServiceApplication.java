package com.tms.outbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entrypoint of the Outbox Processing Service (SVC-002, story SCAFFOLD-SVC-002).
 *
 * <p>Layout follows the C4 hexagonal split: {@code domain} (entity, ports, domain services), {@code
 * application} (processing/cleanup/monitoring services), {@code infrastructure} (JPA, publishers,
 * scheduling, messaging, observability), {@code api}/{@code health} (presentation) and the
 * cross-cutting {@code web}/{@code security}/{@code config} packages.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OutboxServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OutboxServiceApplication.class, args);
  }
}
