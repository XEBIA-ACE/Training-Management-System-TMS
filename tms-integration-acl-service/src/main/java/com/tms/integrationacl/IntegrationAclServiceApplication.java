package com.tms.integrationacl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entrypoint of the External Integrations ACL Service (SVC-003). */
@SpringBootApplication
public class IntegrationAclServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(IntegrationAclServiceApplication.class, args);
  }
}
