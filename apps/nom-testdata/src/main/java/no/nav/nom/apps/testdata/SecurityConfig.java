package no.nav.nom.apps.testdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN_AUTHORITY = "NOM_ADMIN";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${nom.security.admin-group-id}") String adminGroupId) throws Exception {
        Assert.hasText(adminGroupId, "Admin group must be configured");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object groups = jwt.getClaims().get("groups");
            return groups instanceof Collection<?> values && values.contains(adminGroupId)
                    ? List.of(new SimpleGrantedAuthority(ADMIN_AUTHORITY)) : List.of();
        });

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/internal/health/liveness", "/internal/health/readiness",
                                "/internal/prometheus", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/swagger-config").permitAll()
                        .anyRequest().hasAuthority(ADMIN_AUTHORITY))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    // Denne sjekker at tokenet er utstedt av en av de pre-autorisert appene og til denne appen, og at det er et delegert bruker-token (ikke et app-token)
    @Bean
    JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties,
            @Value("${nom.security.pre-authorized-apps}") String preAuthorizedApps) {
        var settings = properties.getJwt();
        Assert.hasText(settings.getIssuerUri(), "JWT issuer must be configured");
        Assert.hasText(settings.getJwkSetUri(), "JWKS URI must be configured");
        Assert.notEmpty(settings.getAudiences(), "JWT audience must be configured");
        Set<String> allowedClients = allowedClients(preAuthorizedApps);

        var decoder = NimbusJwtDecoder.withJwkSetUri(settings.getJwkSetUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(settings.getIssuerUri()),
                jwt -> jwt.getAudience().stream().anyMatch(settings.getAudiences()::contains)
                        ? OAuth2TokenValidatorResult.success() : invalidToken(),
                jwt -> isDelegatedUserToken(jwt, allowedClients)
                        ? OAuth2TokenValidatorResult.success() : invalidToken()));
        return decoder;
    }

    private static boolean isDelegatedUserToken(Jwt jwt, Set<String> allowedClients) {
        Object scopes = jwt.getClaims().get("scp");
        Object objectId = jwt.getClaims().get("oid");
        Object authorizedParty = jwt.getClaims().get("azp");
        return jwt.getExpiresAt() != null
                && !"app".equals(jwt.getClaims().get("idtyp"))
                && scopes instanceof String scope
                && List.of(scope.split("\\s+")).contains("defaultaccess")
                && objectId instanceof String id && !id.isBlank()
                && authorizedParty instanceof String client && allowedClients.contains(client);
    }

    private static Set<String> allowedClients(String preAuthorizedApps) {
        var entries = JsonMapper.builder().build().readTree(preAuthorizedApps);
        Assert.isTrue(entries != null && entries.isArray(), "Pre-authorized apps must be an array");
        Set<String> clients = new HashSet<>();
        for (var entry : entries) {
            var clientId = entry.get("clientId");
            Assert.isTrue(clientId != null && clientId.isString() && !clientId.asString().isBlank(),
                    "Pre-authorized apps must contain clientId");
            clients.add(clientId.asString());
        }
        Assert.notEmpty(clients, "At least one pre-authorized app must be configured");
        return Set.copyOf(clients);
    }

    private static OAuth2TokenValidatorResult invalidToken() {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Token does not meet the resource server requirements", null));
    }
}
