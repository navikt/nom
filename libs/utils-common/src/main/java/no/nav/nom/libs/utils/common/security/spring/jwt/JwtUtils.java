package no.nav.nom.libs.utils.common.security.spring.jwt;


import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Set;

public class JwtUtils {

    public static JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties, Set<String> acceptedAudience) {
        val jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(properties.getJwt().getIssuerUri());
        jwtDecoder.setJwtValidator(oAuth2TokenValidator(properties, acceptedAudience));
        return jwtDecoder;
    }

    public static JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
        val jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(properties.getJwt().getIssuerUri());
        return jwtDecoder;
    }

    private static OAuth2TokenValidator<Jwt> oAuth2TokenValidator(OAuth2ResourceServerProperties properties, Set<String> acceptedAudience) {
        val issuerValidator = JwtValidators.createDefaultWithIssuer(properties.getJwt().getIssuerUri());
        val audienceValidator = new AudienceValidator(acceptedAudience);
        return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);
    }

    @RequiredArgsConstructor
    private static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final Set<String> acceptedAudience;

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            val accepted = token.getAudience().stream().anyMatch(acceptedAudience::contains);

            if (accepted) {
                return OAuth2TokenValidatorResult.success();
            } else {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", String.format("None of required audience values '%s' found in token", acceptedAudience), null));
            }
        }
    }

}
