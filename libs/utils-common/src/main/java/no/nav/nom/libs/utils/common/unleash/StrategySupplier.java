package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.strategy.Strategy;

@FunctionalInterface
public interface StrategySupplier {
    Strategy[] strategies();
}
