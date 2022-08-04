package no.nav.nom.apps.navident.common;

import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StartupProbeHealthIndicator implements HealthIndicator {

    private ApplicationAvailability applicationAvailability;

    @Override
    public Health health() {
        boolean isAlive = applicationAvailability.getLivenessState() == LivenessState.CORRECT;
        boolean isReady = applicationAvailability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;

        Health.Builder status = Health.up();
        if (! (hasAllSourceEventsBeenRead() && isAlive && isReady) ) {
            status = Health.down();
        }
        return status.build();
    }

    private boolean hasAllSourceEventsBeenRead() {
        // TODO: Implement me
        return true;
    }
}

