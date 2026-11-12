package com.tms.crosscutting.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata; enabled with {@code TMS_OPENAPI_ENABLED}. */
@Configuration
public class OpenApiConfig {

  static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI tmsOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Training Management Core Service API")
                .version("1.0.0")
                .description("Core backend API for the Training Management System."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
