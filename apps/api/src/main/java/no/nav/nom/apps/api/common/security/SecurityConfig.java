package no.nav.nom.apps.api.common.security;

import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
    JwtDecoder jwtDecoder(Environment environment, OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
        final var audience = new HashSet<>(List.of(environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.accepted-audience", String[].class)));
        return JwtUtils.jwtDecoder(oAuth2ResourceServerProperties, audience);
    }

    private void configureInternalPaths(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        registry.mvcMatchers("/internal/**").permitAll();
    }

    private void configureSwaggerPaths(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        registry.mvcMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/v3/api-docs/**"
        ).permitAll();
    }

    private void configureGraphqlPaths(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        registry.mvcMatchers("/graphql", "/graphql/schema", "/graphiql/**").permitAll();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTFilter jwtFilter) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeRequests(this::configureInternalPaths) // Authorize all default/internal paths
                .authorizeRequests(this::configureSwaggerPaths) // Authorize all swagger/openapi paths
                .authorizeRequests(this::configureGraphqlPaths) // Authorize all graphql paths
                .authorizeRequests(requests -> requests.anyRequest().fullyAuthenticated())// Catch all pattern for other requests
                .addFilterBefore(jwtFilter, RequestHeaderAuthenticationFilter.class)
                .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        return http.build();
    }
}
