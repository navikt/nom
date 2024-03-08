package no.nav.nom.apps.api.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Prøve å pause Spring shutdown i k8s, slik at k8s rekker å 'stoppe' å ta imot trafikk før vi faktisk stopper.
 * Ref problembeskrivelsen her: https://nav-it.slack.com/archives/C5KUST8N6/p1709557840950139
 *
 * Oppskrift herfra - https://github.com/spring-projects/spring-boot/issues/20995
 * Håper issuen over blir løst i selve Spring Boot, slik at vi kan fjerne denne klassen.
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
            SECONDS.sleep(10); //
            // The sleep value should be, of course, configurable through a property (maybe can have a default of 10s).
            // The important rule to whoever sets it in their project is that it is bigger than the periodSeconds of the readiness probe,
            // so that it ensures that it gives enough time for the probe to run at least once before entering the graceful shutdown
            // (as a rule of thumb I would suggest 2 x periodSeconds)
            // Ta også hensyn til failureThreshold
        } catch (InterruptedException e) {
            log.error("Delay before commencing graceful shutdown interrupted", e);
        }
        log.info("Delay before commencing graceful shutdown finished");
    }

    @Override
    public Health getHealth(boolean includeDetails) {
        return shuttingDown ? Health.outOfService().build() : super.getHealth(includeDetails);
    }
}
