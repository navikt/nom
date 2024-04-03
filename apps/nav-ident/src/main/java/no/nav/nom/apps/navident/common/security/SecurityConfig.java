package no.nav.nom.apps.navident.common.security;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationProvider jwtAuthenticationProvider(OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
        var jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(oAuth2ResourceServerProperties.getJwt().getIssuerUri());

        var issuerValidator = JwtValidators.createDefaultWithIssuer(oAuth2ResourceServerProperties.getJwt().getIssuerUri());
        var audienceValidator = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, (aud) -> aud != null && !Collections.disjoint(aud,
                oAuth2ResourceServerProperties.getJwt().getAudiences()));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

        return new JwtAuthenticationProvider(jwtDecoder);
    }

    @Bean
    public JwtAuthenticationConverter customJwtAuthenticationConverter(GrantedAuthoritiesConverter grantedAuthoritiesConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        converter.setPrincipalClaimName("NAVident");
        return converter;
    }
}
