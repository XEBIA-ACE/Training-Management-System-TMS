package com.tms.integrationacl.security;

import com.tms.integrationacl.config.ExternalIntegrationsAclProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.util.StringUtils;

/**
 * Stateless OAuth2 Resource Server (bearer JWT from the central IdP).
 *
 * <ul>
 *   <li>{@code GET /actuator/health/**}: public (Kubernetes liveness/readiness probes).
 *   <li>{@code /actuator/metrics/**}: authenticated; other Actuator endpoints denied.
 *   <li>Everything else (e.g. {@code /api/v1/integration-acl/**}): authenticated.
 * </ul>
 *
 * <p>JWT validation is only enabled when {@code TMS_ACL_JWT_JWK_SET_URI} or {@code
 * TMS_ACL_JWT_ISSUER_URI} is set; otherwise every protected request is rejected with 401.
 *
 * <p>TODO SCAFFOLD-SVC-003: scope enforcement ({@code tms.acl.read}, {@code tms.acl.write}, ...),
 * audience validation and rate limiting belong to the stories implementing the ACL endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, ExternalIntegrationsAclProperties properties) throws Exception {
    HttpStatusEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
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
              auth.requestMatchers("/actuator/metrics", "/actuator/metrics/**").authenticated();
              auth.requestMatchers("/actuator/**").denyAll();
              auth.anyRequest().authenticated();
            })
        .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorized));

    JwtDecoder jwtDecoder = jwtDecoder(properties.security().jwt());
    if (jwtDecoder != null) {
      http.oauth2ResourceServer(
          oauth2 ->
              oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)).authenticationEntryPoint(unauthorized));
    }
    return http.build();
  }

  private static JwtDecoder jwtDecoder(ExternalIntegrationsAclProperties.Jwt jwt) {
    if (StringUtils.hasText(jwt.jwkSetUri())) {
      return NimbusJwtDecoder.withJwkSetUri(jwt.jwkSetUri()).build();
    }
    if (StringUtils.hasText(jwt.issuerUri())) {
      return NimbusJwtDecoder.withIssuerLocation(jwt.issuerUri()).build();
    }
    return null;
  }
}
