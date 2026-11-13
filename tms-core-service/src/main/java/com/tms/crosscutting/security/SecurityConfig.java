package com.tms.crosscutting.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless OAuth2 Resource Server (bearer JWT) configuration with transport-level RBAC.
 *
 * <ul>
 *   <li>{@code /actuator/health/**}: public (rate limiting is applied at the gateway).
 *   <li>{@code /actuator/metrics/**}: {@code SUPER_ADMIN} or {@code OPS_MONITOR}.
 *   <li>{@code /api/admin/**}: {@code SUPER_ADMIN}.
 *   <li>{@code POST /api/auth/login|refresh}: public only when legacy auth is enabled.
 *   <li>{@code /api/v1/customers/**} (US-001): authenticated bearer token. GAP(US-001): no
 *       customer-specific roles/permissions are specified, so no role restriction is applied.
 *   <li>Everything else: authenticated bearer token.
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] OPENAPI_PATHS = {
    "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      TmsSecurityProperties properties,
      TmsJwtAuthenticationConverter jwtAuthenticationConverter,
      SecurityErrorHandler errorHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> {
              auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
              auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                  .permitAll();
              auth.requestMatchers("/actuator/metrics", "/actuator/metrics/**")
                  .hasAnyRole(TmsRoles.SUPER_ADMIN, TmsRoles.OPS_MONITOR);
              auth.requestMatchers("/actuator/**").denyAll();
              if (properties.legacyAuth().enabled()) {
                auth.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh")
                    .permitAll();
              } else {
                auth.requestMatchers("/api/auth/login", "/api/auth/refresh").denyAll();
              }
              if (properties.openapiPublic()) {
                auth.requestMatchers(OPENAPI_PATHS).permitAll();
              }
              auth.requestMatchers("/api/admin/**").hasRole(TmsRoles.SUPER_ADMIN);
              auth.requestMatchers("/api/v1/customers", "/api/v1/customers/**").authenticated();
              auth.anyRequest().authenticated();
            })
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(errorHandler))
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler));
    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder(TmsSecurityProperties properties) {
    return JwtDecoderFactory.create(properties.jwt());
  }

  @Bean
  static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    handler.setPermissionEvaluator(new RbacPermissionEvaluator());
    return handler;
  }
}
