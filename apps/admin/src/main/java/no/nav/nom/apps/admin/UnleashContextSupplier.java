package no.nav.nom.apps.admin;

import io.getunleash.UnleashContext;
import io.getunleash.UnleashContextProvider;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.utils.common.unleash.ContextSupplier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class UnleashContextSupplier implements ContextSupplier {
    public static String NAVIDENT_CLAIM_NAME = "NAVident";

    @Override
    public UnleashContextProvider context() {
        return () -> {
            var navident = getAuthenticatedNavIdent();

            return new UnleashContext
                .Builder(UnleashContextProvider.getDefaultProvider().getContext())
                .userId(navident.orElse("Unauthenticated user"))
                .build();
        };
    }

    private static Optional<String> getAuthenticatedNavIdent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Optional.ofNullable(jwt.getClaimAsString(NAVIDENT_CLAIM_NAME));
    }
}
