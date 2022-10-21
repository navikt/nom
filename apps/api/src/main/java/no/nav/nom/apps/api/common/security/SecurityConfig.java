package no.nav.nom.apps.api.common.security;

import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import java.util.HashSet;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableGlobalMethodSecurity(
        prePostEnabled = true
)
public class SecurityConfig {

    @Bean
    JwtAuthenticationProvider jwtAuthenticationProvider(Environment environment, OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
        final var audience = new HashSet<>(List.of(environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.accepted-audience", String[].class)));
        return new JwtAuthenticationProvider(JwtUtils.jwtDecoder(oAuth2ResourceServerProperties, audience));
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .mvcMatchers("/internal/**") // Ignore internal paths
                .mvcMatchers("/graphiql/**") // Ignore graphiql path
                .mvcMatchers(
                        "/api", // for redirect til swagger(??)
                        "/swagger-ui",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**"
                );
    }

    @Bean
    @Order(1)
    public SecurityFilterChain graphqlFilterChain(HttpSecurity http, JWTFilter jwtFilter) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeRequests(requests -> requests.antMatchers("/graphql").permitAll())
                .addFilterBefore(jwtFilter, RequestHeaderAuthenticationFilter.class)
                .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTFilter jwtFilter) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeRequests(requests -> requests.anyRequest().fullyAuthenticated())
                .addFilterBefore(jwtFilter, RequestHeaderAuthenticationFilter.class)
                .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        return http.build();
    }
}
