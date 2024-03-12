package no.nav.nom.apps.api.authorize;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public boolean kanRedigereRessurs(String navident) {
        if (unleash.isEnabled(UNLEASH_REDIGERE_RESSURS_BRUK_NY_AUTH_RESOLVER)) {
            Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());
            return lederNavident.isPresent() && authorizedNavident.kanBrukerRedigereRessurs(lederNavident.get(), navident);
        } else {
            return true;
        }
    }
}
