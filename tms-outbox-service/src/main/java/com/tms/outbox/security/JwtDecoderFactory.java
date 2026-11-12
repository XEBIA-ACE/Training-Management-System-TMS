package com.tms.outbox.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.util.StringUtils;

/** Builds the bearer-token {@link JwtDecoder} from {@link OutboxSecurityProperties.Jwt}. */
final class JwtDecoderFactory {

  private JwtDecoderFactory() {}

  static JwtDecoder create(OutboxSecurityProperties.Jwt properties) {
    String issuer = properties.issuerUri();
    if (StringUtils.hasText(properties.jwkSetUri())) {
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
      decoder.setJwtValidator(validator(properties));
      return decoder;
    }
    if (StringUtils.hasText(issuer)) {
      // Lazy so startup does not depend on the IdP being reachable.
      return new SupplierJwtDecoder(
          () -> {
            NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
            decoder.setJwtValidator(validator(properties));
            return decoder;
          });
    }
    if (properties.required()) {
      throw new IllegalStateException(
          "No JWT issuer configured: set TMS_OUTBOX_JWT_ISSUER_URI and/or TMS_OUTBOX_JWT_JWK_SET_URI");
    }
    return token -> {
      throw new JwtException("No JWT issuer configured; bearer tokens are rejected");
    };
  }

  private static OAuth2TokenValidator<Jwt> validator(OutboxSecurityProperties.Jwt properties) {
    List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    validators.add(
        StringUtils.hasText(properties.issuerUri())
            ? JwtValidators.createDefaultWithIssuer(properties.issuerUri())
            : JwtValidators.createDefault());
    List<String> audiences = properties.audiences().stream().filter(StringUtils::hasText).toList();
    if (!audiences.isEmpty()) {
      validators.add(
          new JwtClaimValidator<List<String>>(
              JwtClaimNames.AUD, aud -> aud != null && aud.stream().anyMatch(audiences::contains)));
    }
    return new DelegatingOAuth2TokenValidator<>(validators);
  }
}
