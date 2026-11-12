package com.tms.crosscutting.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

class JwtDecoderFactoryTest {

  @Test
  void failsFastWhenNoIssuerConfiguredAndRequired() {
    TmsSecurityProperties properties = TmsJwtAuthenticationConverterTest.bind(Map.of());

    assertThatThrownBy(() -> JwtDecoderFactory.create(properties.jwt()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TMS_JWT_ISSUER_URI");
  }

  @Test
  void rejectsAllTokensWhenNoIssuerConfiguredAndNotRequired() {
    TmsSecurityProperties properties =
        TmsJwtAuthenticationConverterTest.bind(Map.of("tms.security.jwt.required", "false"));

    assertThatThrownBy(() -> JwtDecoderFactory.create(properties.jwt()).decode("token"))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void usesJwkSetUriOrLazyIssuerDiscovery() {
    TmsSecurityProperties jwks =
        TmsJwtAuthenticationConverterTest.bind(
            Map.of("tms.security.jwt.jwk-set-uri", "https://idp.invalid/keys"));
    TmsSecurityProperties issuer =
        TmsJwtAuthenticationConverterTest.bind(
            Map.of("tms.security.jwt.issuer-uri", "https://idp.invalid/tenant"));

    assertThat(JwtDecoderFactory.create(jwks.jwt())).isInstanceOf(NimbusJwtDecoder.class);
    assertThat(JwtDecoderFactory.create(issuer.jwt())).isInstanceOf(SupplierJwtDecoder.class);
  }
}
