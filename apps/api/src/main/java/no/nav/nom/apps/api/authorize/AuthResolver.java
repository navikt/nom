package no.nav.nom.apps.api.authorize;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import org.springframework.stereotype.Service;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.loggedInUserHasAccessScope;

@Service
@Slf4j
public class AuthResolver {
    private final AuthorizedNavident authorizedNavident;

    public AuthResolver(AuthorizedNavident authorizedNavident) {
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
        return loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
            authorizedNavident.kanBrukerRedigereRessurs(getAuthenticatedNavIdent(), navident);
    }

    /**
     * Sjekker om inloggede bruker er lik ressursen som skal redigeres.
     * @param navident
     * @return true hvis tilgang, false hvis ikke
     */

    public boolean kanSelvbetjene(String navident) {
        return getAuthenticatedNavIdent().equals(navident);
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
        return loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
            authorizedNavident.kanBrukerOppretteRessurs(getAuthenticatedNavIdent(), orgenhetId);
    }

    public boolean erRessursInNomWriteDelegertGroup(String ressursNavident) {
        return authorizedNavident.getRessurserWhichCanBeDelegatedTo()
            .stream()
            .anyMatch(ressurs -> ressurs.navident().equals(ressursNavident));
    }

}
