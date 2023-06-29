package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.strategy.Strategy;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ByClusterStrategy implements Strategy {

    @Override
    public String getName() {
        return "byCluster";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        return ToggleChecker.isToggleEnabled("cluster", parameters, ByClusterStrategy::isCluster);
    }

    private static boolean isCluster(String toggleNamespace) {
        return Objects.equals(toggleNamespace, Optional.ofNullable(System.getenv("NAIS_CLUSTER_NAME")).orElse("local"));
    }
}