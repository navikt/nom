package no.nav.nom.apps.api.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static org.springframework.security.core.context.SecurityContextHolder.createEmptyContext;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private final static Logger log = LoggerFactory.getLogger(JWTFilter.class);

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final BearerTokenResolver tokenResolver = new DefaultBearerTokenResolver(); // Leverage Spring to resolve the Bearer-token for us.

    public JWTFilter(
            JwtAuthenticationProvider jwtAuthenticationProvider,
            AZPAuthoritiesConverter authoritiesConverter
    ) {
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter); // This should probably be configured throught the SecurityConfigurerAdapter/FilterChain

        this.jwtAuthenticationProvider.setJwtAuthenticationConverter(authenticationConverter);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            Optional.ofNullable(tokenResolver.resolve(request))
                    .map(BearerTokenAuthenticationToken::new)
                    .map(jwtAuthenticationProvider::authenticate)
                    .ifPresent(e -> {
                        final var context = createEmptyContext();
                        context.setAuthentication(e);
                        SecurityContextHolder.setContext(context);
                    });
        } catch (RuntimeException e) {
            // Consider catching OAuth2AuthenticationException or AuthenticationException in stead
            log.warn("Failed to validate JWT token", e);
        }

        chain.doFilter(request, response);
    }
}
