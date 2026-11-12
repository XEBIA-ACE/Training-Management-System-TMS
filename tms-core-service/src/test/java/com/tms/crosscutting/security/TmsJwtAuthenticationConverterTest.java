package com.tms.crosscutting.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class TmsJwtAuthenticationConverterTest {

  private final TmsSecurityProperties properties = bind(Map.of());

  @Test
  void mapsRolesGroupsAndAppRolesClaimsToRoleAuthorities() {
    Jwt jwt =
        jwt(
            Map.of(
                "roles",
                List.of("SUPER_ADMIN"),
                "groups",
                List.of("OPS_MONITOR", " "),
                "app_roles",
                "ROLE_SUPER_ADMIN",
                "scp",
                "ignored"));

    AbstractAuthenticationToken token = new TmsJwtAuthenticationConverter(properties).convert(jwt);

    assertThat(token.getName()).isEqualTo("user-1");
    assertThat(token.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_OPS_MONITOR");
  }

  @Test
  void roleClaimNamesAreConfigurable() {
    TmsSecurityProperties custom = bind(Map.of("tms.security.jwt.role-claims", "custom_roles"));
    Jwt jwt = jwt(Map.of("roles", List.of("SUPER_ADMIN"), "custom_roles", List.of("OPS_MONITOR")));

    assertThat(new TmsJwtAuthenticationConverter(custom).convert(jwt).getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_OPS_MONITOR");
  }

  @Test
  void resolvesTenantFromTidClaim() {
    Jwt jwt = jwt(Map.of("tid", "tenant-42"));
    TenantResolver resolver = new TenantResolver(properties);

    assertThat(resolver.resolveTenant(new TmsJwtAuthenticationConverter(properties).convert(jwt)))
        .contains("tenant-42");
    assertThat(resolver.resolveTenant(null)).isEmpty();
  }

  @Test
  void bindsDefaultsAndEnvironmentOverrides() {
    assertThat(properties.jwt().roleClaims()).containsExactly("roles", "groups", "app_roles");
    assertThat(properties.jwt().tenantClaim()).isEqualTo("tid");
    assertThat(properties.jwt().required()).isTrue();
    assertThat(properties.legacyAuth().enabled()).isFalse();

    TmsSecurityProperties overridden =
        bind(
            Map.of(
                "tms.security.jwt.issuer-uri", "https://idp.invalid/tenant",
                "tms.security.jwt.audiences", "api://tms,tms-core",
                "tms.security.legacy-auth.enabled", "true"));
    assertThat(overridden.jwt().issuerUri()).isEqualTo("https://idp.invalid/tenant");
    assertThat(overridden.jwt().audiences()).containsExactly("api://tms", "tms-core");
    assertThat(overridden.legacyAuth().enabled()).isTrue();
  }

  static TmsSecurityProperties bind(Map<String, String> values) {
    return new Binder(new MapConfigurationPropertySource(values))
        .bindOrCreate("tms.security", TmsSecurityProperties.class);
  }

  private static Jwt jwt(Map<String, Object> claims) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user-1")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60));
    claims.forEach(builder::claim);
    return builder.build();
  }
}
