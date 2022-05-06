package no.nav.nom.apps.api.common.security;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import java.util.HashSet;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(
        prePostEnabled = true
)
public class SecurityConfig {
    @Bean
    JwtAuthenticationProvider jwtAuthenticationProvider(Environment environment, OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
        val audience = new HashSet<String>(List.of(environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.accepted-audience", String[].class)));
        return new JwtAuthenticationProvider(JwtUtils.jwtDecoder(oAuth2ResourceServerProperties, audience));
    }
}
