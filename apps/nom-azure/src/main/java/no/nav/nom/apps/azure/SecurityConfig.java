package no.nav.nom.apps.azure;

import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.Arrays;
import java.util.logging.Logger;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private final Logger logger = Logger.getLogger(SecurityConfig.class.getName());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment) throws Exception {

        var common = http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .securityMatcher("/**");

        if (isLocalEnv(environment)) {
            common.authorizeHttpRequests((auth) -> auth
                .anyRequest().permitAll()
            );
            logger.info("Local environment detected. Permit all");
        } else {
            common
                .authorizeHttpRequests((auth) -> auth
                    .requestMatchers(
                        "/internal/**",
                        "v3/api-docs/**",
                        "/error"
                    ).permitAll()
                    .anyRequest().fullyAuthenticated()
                );
            logger.info("non-local environment detected. require authenticated");

        }

        return common
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling((exceptionHandling) ->
                exceptionHandling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                ))
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
        return JwtDecoders.fromIssuerLocation(properties.getJwt().getIssuerUri());
    }

    private boolean isLocalEnv(Environment environment) {
        var lacksPlatformEnvVariable = environment.getProperty("NAIS_CLIENT_ID") == null;
        return lacksPlatformEnvVariable && Arrays.asList(environment.getActiveProfiles()).contains("local");
    }
}
