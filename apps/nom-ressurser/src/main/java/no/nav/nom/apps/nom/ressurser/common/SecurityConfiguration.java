package no.nav.nom.apps.nom.ressurser.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityMatcher("/**")
            .authorizeHttpRequests((auth) -> auth
                .requestMatchers(
                    // Default/internal paths
                    "/internal/**",
                    // Ovrige endepunkt som ikke skal autentiseres
                    "/api/legacy/ressurs/**",
                    "/api/v1/ressursdata/**",
                    // https://github.com/spring-projects/spring-boot/issues/33934
                    "/error"
                ).permitAll()
                .anyRequest().fullyAuthenticated()
            )
            .exceptionHandling((exceptionHandling) ->
                exceptionHandling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                ))
            .build();
    }
}
