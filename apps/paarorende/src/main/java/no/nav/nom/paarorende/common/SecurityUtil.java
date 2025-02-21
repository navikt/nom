package no.nav.nom.paarorende.common;

import no.nav.nom.paarorende.consumer.NomApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;


@Service
public class SecurityUtil {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Environment environment;
    private final NomApiClient nomApiClient;

    private static final String NAVIDENT = "NAVident";
    private static final String GROUPS = "groups";

    @Value("${app.security.nom-admin}")
    private String admin;

    public SecurityUtil(Environment environment, NomApiClient nomApiClient) {
        this.environment = environment;
        this.nomApiClient = nomApiClient;
    }

    public void assertAuthorized() throws AccessDeniedException {
        if (!isAuthenticated() || getNavIdent() == null) {
            throw new AccessDeniedException("Unauthorized");
        }
    }

    public void assertAuthorized(String navident) throws AccessDeniedException {
        if (!isAuthenticated() || !navident.equals(getNavIdent())) {
            throw new AccessDeniedException("Unauthorized");
        }
    }

    public void assertAuthorizedAccessTo(String navident) throws AccessDeniedException {
        // debugging purpose
        log.info("getUserNavident(): {}", getUserNavident());
        log.info("isAuthenticated(): {}", isAuthenticated());
        log.info("getNavIdent(): {}", getNavIdent());
        log.info("hasAccess(): {}", hasAccess(getNavIdent(), navident));

        if (!isAuthenticated() || getNavIdent() == null || !hasAccess(getNavIdent(), navident)) {
            log.error("Unauthorized access to {}", navident);
            throw new AccessDeniedException("Unauthorized");
        }

        log.info("assertAuthorizedAccessTo OK");
    }

    public String getNavIdent() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("local")) {
            return "testuser";
        }

        try {
            return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString(NAVIDENT);
        } catch (Exception e) {
            throw new AccessDeniedException("Unauthorized");
        }
    }

    private boolean isAdmin() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("local")) {
            return true;
        }

        try {
            return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getClaimAsStringList(GROUPS)
                .contains(admin);
        } catch (Exception e) {
            throw new AccessDeniedException("Unauthorized");
        }
    }

    private boolean isAuthenticated() {
        return  Arrays.asList(environment.getActiveProfiles()).contains("local") ||
            SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
    }

    private boolean hasAccess(String bruker, String ressurs) {
        return nomApiClient.hasAccess(bruker, ressurs) || isAdmin() || bruker.equals(ressurs);
    }

    private Optional<String> getUserNavident() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            var azp_name = token.getTokenAttributes().get("NAVident");
            return Optional.ofNullable(azp_name).map(Objects::toString);
        }
        return Optional.empty();
    }
}
