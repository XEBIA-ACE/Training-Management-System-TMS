package com.tms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entrypoint of the Training Management Core Service (modular monolith). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TmsCoreServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TmsCoreServiceApplication.class, args);
  }
}
