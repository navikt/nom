package no.nav.nom.paarorende.common;

import no.nav.nom.paarorende.consumer.NomApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service
public class SecurityUtil {

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
        if (!isAuthenticated() || getNavIdent() == null || !isLeaderFor(getNavIdent(), navident) && !isAdmin() && !navident.equals(getNavIdent())) {
            throw new AccessDeniedException("Unauthorized");
        }
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

    private boolean isLeaderFor(String lederNavident, String navident) {
        var ledere = nomApiClient.getLedereFor(navident);

        if (ledere.stream().anyMatch(lederNavident::equals)) {
            return true;
        }

        var lederesLedere = ledere.stream().map(nomApiClient::getLedereFor).flatMap(List::stream);

        return lederesLedere.anyMatch(lederNavident::equals);
    }
}
