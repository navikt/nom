package no.nav.nom.apps.api.common;

import io.getunleash.UnleashContext;
import io.getunleash.UnleashContextProvider;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.libs.utils.common.unleash.ContextSupplier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class UnleashContextSupplier implements ContextSupplier {

    @Override
    public UnleashContextProvider context() {
        return () -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            Optional<String> navident = auth == null ? Optional.empty() : SecurityUtil.getNavIdent(auth);

            return new UnleashContext
                .Builder(UnleashContextProvider.getDefaultProvider().getContext())
                .userId(navident.orElse("Unauthenticated user"))
                .build();
        };
    }
}
