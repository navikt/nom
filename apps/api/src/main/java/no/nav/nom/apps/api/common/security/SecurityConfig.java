package no.nav.nom.apps.api.common.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTFilter jwtFilter) throws Exception {

        RequestMatcher anyRequestMatcher = AnyRequestMatcher.INSTANCE;

        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .securityMatcher(anyRequestMatcher)
            .authorizeHttpRequests((auth) -> auth
                .requestMatchers(
                    // Default/internal paths
                    "/internal/**",
                    //  Swagger/openapi paths
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/v3/api-docs/**",
                    // Graphql paths
                    "/graphql",
                    "/graphql/schema",
                    "/graphiql/**",
                    // https://github.com/spring-projects/spring-boot/issues/33934
                    "/error"
                ).permitAll()
                .anyRequest().fullyAuthenticated()
            )
            .addFilterBefore(jwtFilter, RequestHeaderAuthenticationFilter.class)
            .exceptionHandling((exceptionHandling) ->
                exceptionHandling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                ))
            .build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(final CorsRegistry registry) {
                registry.addMapping("/graphql/**")
                    .allowedOriginPatterns("https://*.svc.local", "https://*.nav.no", "https://navno.sharepoint.com", "http://localhost:[*]")
                    //                    .allowedOrigins("https://nom.nav.no","https://nom.dev.nav.no","https://navno.sharepoint.com")
                    //                    .allowedOrigins(CorsConfiguration.ALL)    // BRUK DENNE HVIS DET OVER IKKE FUNKER
                    .allowedHeaders(CorsConfiguration.ALL)
                    .allowCredentials(true)
                    .allowedMethods(CorsConfiguration.ALL);
            }
        };
    }
}
