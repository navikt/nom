package no.nav.nom.apps.api.authorize;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.loggedInUserHasAccessScope;

@Service
@Slf4j
public class AuthResolver {
    public static final String UNLEASH_REDIGERE_RESSURS_BRUK_NY_AUTH_RESOLVER = "nom.redigere_ressurs.bruk_ny_auth_resolver";
    private final Unleash unleash;
    private final AuthorizedNavident authorizedNavident;

    public AuthResolver(Unleash unleash, AuthorizedNavident authorizedNavident) {
        this.unleash = unleash;
        this.authorizedNavident = authorizedNavident;
    }

    /**
     * Sjekker om innloggede bruker har tilgang til å redigere ressurs.
     * NOM_ADMIN har alltid tilgang.
     * Lederere eller delegerte ledere for angitt ressurs har tilgang.
     * Se mer i AuthorizedNavident.
     *
     * @param navident
     * @return true hvis tilgang, false hvis ikke
     */
    public boolean kanRedigereRessurs(String navident) {
        if (unleash.isEnabled(UNLEASH_REDIGERE_RESSURS_BRUK_NY_AUTH_RESOLVER)) {
            return loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
                authorizedNavident.kanBrukerRedigereRessurs(getAuthenticatedNavIdent(), navident);
        } else {
            return true;
        }
    }

    /**
     * Sjekker om innloggede bruker har tilgang til å opprette ressurs i angitt orgenhet.
     * NOM_ADMIN har alltid tilgang.
     * Lederere eller delegerte ledere for angitt enhet har tilgang.
     * Se mer i AuthorizedNavident.
     *
     * @param orgenhetId
     * @return true hvis tilgang, false hvis ikke
     */
    public boolean kanOppretteRessurs(String orgenhetId) {
        if (unleash.isEnabled(UNLEASH_REDIGERE_RESSURS_BRUK_NY_AUTH_RESOLVER)) {
            return loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
                authorizedNavident.kanBrukerOppretteRessurs(getAuthenticatedNavIdent(), orgenhetId);
        } else {
            return true;
        }
    }

}
