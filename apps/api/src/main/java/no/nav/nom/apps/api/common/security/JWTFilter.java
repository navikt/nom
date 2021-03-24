package no.nav.nom.apps.api.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static java.util.function.Predicate.not;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            Optional.ofNullable(request.getHeader("Authorization"))
                    .filter(not(String::isEmpty))
                    .map(t -> t.replace("Bearer ", ""))
                    .map(BearerTokenAuthenticationToken::new)
                    .map(jwtAuthenticationProvider::authenticate)
                    .ifPresent(e -> {
                        var context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(e);
                        SecurityContextHolder.setContext(context);
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to validate JWT token", e);
        }
        filterChain.doFilter(request, response);
    }
}
