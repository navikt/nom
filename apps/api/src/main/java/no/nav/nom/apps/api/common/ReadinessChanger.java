package no.nav.nom.apps.api.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static no.nav.nom.apps.api.dvh.DvhRestConsumer.CACHE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadinessChanger {
    private final ApplicationEventPublisher appEventPublisher;

    @EventListener
    public void onEvent(AvailabilityChangeEvent<ReadinessState> event) {
        /*
            Litt hacky, fant ingen gode hookin punkter.
            Kunne også brukt readiness, men tenker når startup probes kommer på plass, så kan man bruke startup probes mot readiness endpoint.
         */
        if (!CACHE.equals(event.getSource()) && event.getState() == ReadinessState.ACCEPTING_TRAFFIC) {
            log.info("Marking pod as REFUSING_TRAFFIC. Waiting for cache population.");
            AvailabilityChangeEvent.publish(appEventPublisher, "n/a", ReadinessState.REFUSING_TRAFFIC);
        }
    }
}
