package no.nav.nom.paarorende.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;


@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public JwtAuthenticationConverter customJwtAuthenticationConverter(GrantedAuthoritiesConverter grantedAuthoritiesConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        converter.setPrincipalClaimName("NAVident");
        return converter;
    }

}