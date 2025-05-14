package no.nav.nom.apps.api.authorize;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public boolean kanRessursRedigereOrgenhet(String ressursNavident, String orgenhetId) {
        List<IOrgenhet> editableOrgenheter = authorizedNavident.orgenheterLederKanRedigere(ressursNavident);
        return editableOrgenheter.stream().anyMatch(orgenhet -> orgenhet.getId().equals(orgenhetId));
    }

    public boolean kanRessursRedigereNoenAvOrgenhetene(String ressursNavident, Set<String> orgenhetIds) {
        var editableOrgenhetIds = authorizedNavident.orgenheterLederKanRedigere(ressursNavident).stream().map(IOrgenhet::getId).collect(Collectors.toSet());
        return !Collections.disjoint(editableOrgenhetIds, orgenhetIds);
    }
}
