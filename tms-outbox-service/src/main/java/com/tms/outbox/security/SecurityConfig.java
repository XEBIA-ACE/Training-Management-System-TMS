package com.tms.outbox.security;

import com.tms.outbox.api.OutboxScopes;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless OAuth2 Resource Server (bearer JWT from the central IdP).
 *
 * <ul>
 *   <li>{@code GET /health}, {@code /health/ready}, {@code /health/live}, {@code /metrics} and the
 *       Actuator health/prometheus endpoints: public (Kubernetes probes / scraping; protect at the
 *       network level per the API insights).
 *   <li>{@code /actuator/metrics/**}: scope {@code outbox:read}; other Actuator endpoints denied.
 *   <li>{@code /api/v1/outbox/**}: authenticated; per-endpoint scopes via {@code @PreAuthorize}
 *       ({@link OutboxScopes}).
 * </ul>
 *
 * <p>TODO SCAFFOLD-SVC-002: rate limiting (429 + Retry-After) is described in the export but its
 * limits are unspecified; expected to be applied at the gateway.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> {
              auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
              auth.requestMatchers(
                      HttpMethod.GET,
                      "/health",
                      "/health/ready",
                      "/health/live",
                      "/metrics",
                      "/actuator/health",
                      "/actuator/health/**",
                      "/actuator/prometheus")
                  .permitAll();
              auth.requestMatchers("/actuator/metrics", "/actuator/metrics/**")
                  .hasAuthority("SCOPE_" + OutboxScopes.READ);
              auth.requestMatchers("/actuator/**").denyAll();
              auth.anyRequest().authenticated();
            })
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> {})
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(errorHandler))
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler));
    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder(OutboxSecurityProperties properties) {
    return JwtDecoderFactory.create(properties.jwt());
  }
}
