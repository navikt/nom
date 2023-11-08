package no.nav.nom.apps.api.common;

import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StartupStateHealthIndicator implements HealthIndicator {
    private final static Logger logger = LoggerFactory.getLogger(StartupStateHealthIndicator.class);

    private final ApplicationAvailability applicationAvailability;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;

    public StartupStateHealthIndicator(ApplicationAvailability applicationAvailability, DvhStartupLoadMonitor dvhStartupLoadMonitor) {
        this.applicationAvailability = applicationAvailability;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
    }

    @Override
    public Health health() {
        boolean isAlive = applicationAvailability.getLivenessState() == LivenessState.CORRECT;
        boolean isReady = applicationAvailability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;

        Health.Builder status = Health.up();
        if (! (hasInitialLoadCompleted() && isAlive && isReady) ) {
            status = new Health.Builder()
                .withDetails(
                    Map.ofEntries(
                        Map.entry("initialLoadCompleted", hasInitialLoadCompleted()),
                        Map.entry("isAlive", isAlive),
                        Map.entry("isReady", isReady)
                    )
                )
                .down();
        }
        return status.build();
    }

    private boolean hasInitialLoadCompleted() {
        return dvhStartupLoadMonitor.hasCompleted();
    }
}
