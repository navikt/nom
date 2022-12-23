package no.nav.nom.apps.navident.common;

import no.nav.nom.apps.navident.eventsource.EventSourceService;
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
    private final EventSourceService eventSourceService;

    public StartupStateHealthIndicator(ApplicationAvailability applicationAvailability, EventSourceService eventSourceService) {
        this.applicationAvailability = applicationAvailability;
        this.eventSourceService = eventSourceService;
    }

    @Override
    public Health health() {
        boolean isAlive = applicationAvailability.getLivenessState() == LivenessState.CORRECT;
        boolean isReady = applicationAvailability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;

        Health.Builder status = Health.up();
        if (! (hasAllSourceEventsBeenRead() && isAlive && isReady) ) {
            status = new Health.Builder()
                    .withDetails(
                            Map.ofEntries(
                                    Map.entry("sourceEventsRead", hasAllSourceEventsBeenRead()),
                                    Map.entry("isAlive", isAlive),
                                    Map.entry("isReady", isReady)
                            )
                    )
                    .down();
            logger.debug("Current startup state: {}", status);
        }
        return status.build();
    }

    private boolean hasAllSourceEventsBeenRead() {
        return eventSourceService.hasAllSourceEventsBeenRead();
    }
}

