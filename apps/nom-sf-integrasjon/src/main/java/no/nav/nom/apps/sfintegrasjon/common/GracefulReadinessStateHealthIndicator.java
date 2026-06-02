package no.nav.nom.apps.sfintegrasjon.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.health.application.ReadinessStateHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Pauser Spring shutdown slik at k8s rekker å stoppe trafikk før appen faktisk stopper.
 * Ref: https://nav-it.slack.com/archives/C5KUST8N6/p1709557840950139
 */
@Slf4j
@Component("readinessStateHealthIndicator")
@Profile({"prod", "dev"})
public class GracefulReadinessStateHealthIndicator extends ReadinessStateHealthIndicator {
    private boolean shuttingDown;

    public GracefulReadinessStateHealthIndicator(ApplicationAvailability availability) {
        super(availability);
    }

    @EventListener
    public void onContextClosedEvent(ContextClosedEvent event) {
        shuttingDown = true;
        try {
            log.info("Readiness probe set as OUT_OF_SERVICE. Delay before commencing graceful shutdown initiated");
            SECONDS.sleep(10);
        } catch (InterruptedException e) {
            log.error("Delay before commencing graceful shutdown interrupted", e);
        }
        log.info("Delay before commencing graceful shutdown finished");
    }

    @Override
    public Health health(boolean includeDetails) {
        return shuttingDown ? Health.outOfService().build() : super.health(includeDetails);
    }
}
