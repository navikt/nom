package no.nav.nom.paarorende.common;

import no.nav.nom.paarorende.consumer.NomApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

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
        if (!isAuthenticated() || getNavIdent() == null || !hasAccess(getNavIdent(), navident)) {
            throw new AccessDeniedException("Unauthorized");
        }
    }

    public boolean canAccessContactInfoForResourcesUnderOrgenhet(String navident, String orgenhetId, List<String> ressurser) {
        var authenticated = isAuthenticated();
        var isAdmin = isAdmin();
        var isLederForOrgenhet = isNavIdentLederForOrgenhet(navident, orgenhetId, ressurser);
        log.info("canAccessContactInfo navident, orgenhetId, authenticated, isAdmin, isLederForOrgenhet : {}, {}, {}, {}, {}", navident, orgenhetId, authenticated, isAdmin, isLederForOrgenhet);
        return isAuthenticated() && (isAdmin() || isNavIdentLederForOrgenhet(navident, orgenhetId, ressurser));
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

    public boolean isAdmin() {
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

    private boolean isNavIdentLederForOrgenhet(String navident, String orgenhetId, List<String> ressurser) {
        return nomApiClient.isNavidentLederForOrgenhet(navident, orgenhetId, ressurser);
    }

    public static String getBehalfOfUserNavident() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken oauthToken = (JwtAuthenticationToken) authentication;
        String onBehalfOfUserNavident = String.valueOf(oauthToken.getTokenAttributes().get("NAVident"));
        return onBehalfOfUserNavident;
    }
}
