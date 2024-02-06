package no.nav.nom.apps.nomremedyintegrasjon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private final static String GROUPS_CLAIM_NAME = "groups";
    public static String NAVIDENT_CLAIM_NAME = "NAVident";

    @Value("${NOM_ADMIN_GROUP_ID}")
    private String ADMIN_AD_GROUP_ID;

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .securityMatcher("/**")
            .authorizeHttpRequests((auth) -> auth
                .requestMatchers(
                    // Default/internal paths
                    "/internal/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/v3/api-docs/**",
                    // https://github.com/spring-projects/spring-boot/issues/33934
                    "/error"
                ).permitAll()
                .anyRequest().fullyAuthenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)
            ))
            .exceptionHandling((exceptionHandling) ->
                exceptionHandling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                ))
            .build();
    }

    @Bean
    public JwtAuthenticationConverter customJwtAuthenticationConverter() {

        var grantedAuthoritiesConverter = new Converter<Jwt, Collection<GrantedAuthority>>() {
            @Override
            public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
                var grantedAuthorities = defaultConverter.convert(jwt);

                var jwtGroups = Optional.ofNullable(jwt.getClaimAsStringList(GROUPS_CLAIM_NAME)).orElse(new ArrayList<>());

                if (jwtGroups.contains(ADMIN_AD_GROUP_ID)) {
                    grantedAuthorities.add(new SimpleGrantedAuthority("ADMIN"));
                }

                return grantedAuthorities;
            }
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        converter.setPrincipalClaimName(NAVIDENT_CLAIM_NAME);
        return converter;
    }

}
