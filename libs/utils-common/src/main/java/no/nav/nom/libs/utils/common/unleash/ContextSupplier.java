package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.UnleashContextProvider;

@FunctionalInterface
public interface ContextSupplier {
    UnleashContextProvider context();
}
