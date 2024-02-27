package no.nav.nom.apps.api.authorize;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class AuthResolver {

    private final AuthorizedNavident authorizedNavident;

    public AuthResolver(AuthorizedNavident authorizedNavident) {
        this.authorizedNavident = authorizedNavident;
    }

    public boolean kanRedigereRessurs(String navident) {
        Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());

        return lederNavident.isEmpty() || !authorizedNavident.kanBrukerRedigereRessurs(lederNavident.get(), navident);
    }
}
