package no.nav.nom.apps.navident.common.security;

import no.nav.nom.apps.navident.common.SwaggerConfig;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SpringSecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SwaggerConfig.SWAGGER_API_URLS).permitAll()
                .requestMatchers("/internal/**").permitAll()
                // https://github.com/spring-projects/spring-boot/issues/33934
                .requestMatchers("/errors").permitAll()
                .anyRequest().fullyAuthenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(handler -> handler.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

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

    @Bean
    public WebClient aadWebClient(AuthenticationProperties authenticationProperties) {
        return WebClient.builder()
            .baseUrl(authenticationProperties.getAad().getIssuerBaseUrl())
            .build();
    }
}
