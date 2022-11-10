package no.nav.nom.libs.utils.common.security.spring.jwt;


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
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(properties.getJwt().getIssuerUri());
        jwtDecoder.setJwtValidator(oAuth2TokenValidator(properties, acceptedAudience));
        return jwtDecoder;
    }

    public static JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
        return JwtDecoders.fromOidcIssuerLocation(properties.getJwt().getIssuerUri());
    }

    private static OAuth2TokenValidator<Jwt> oAuth2TokenValidator(OAuth2ResourceServerProperties properties, Set<String> acceptedAudience) {
        final var issuerValidator = JwtValidators.createDefaultWithIssuer(properties.getJwt().getIssuerUri());
        return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator(acceptedAudience));
    }


    private static OAuth2TokenValidator<Jwt> audienceValidator(Set<String> acceptedAudience) {
        return (token) -> {
            if (token.getAudience().stream().anyMatch(acceptedAudience::contains)) {
                return OAuth2TokenValidatorResult.success();
            } else {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", String.format("None of required audience values '%s' found in token", acceptedAudience), null));
            }
        };
    }

}
