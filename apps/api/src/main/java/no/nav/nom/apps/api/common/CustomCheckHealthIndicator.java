package no.nav.nom.apps.api.common;

import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.EventAggregateListener;
import org.springframework.boot.actuate.availability.AvailabilityStateHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;

@Component
public class CustomCheckHealthIndicator extends AvailabilityStateHealthIndicator {

    private final EventAggregateListener eventAggregateListener;
    private final DvhRestConsumer dvhRestConsumer;

    public CustomCheckHealthIndicator(ApplicationAvailability availability, EventAggregateListener eventAggregateListener, DvhRestConsumer dvhRestConsumer) {
        super(availability, ReadinessState.class, (statusMappings) -> {
            statusMappings.add(ReadinessState.ACCEPTING_TRAFFIC, Status.UP);
            statusMappings.add(ReadinessState.REFUSING_TRAFFIC, Status.OUT_OF_SERVICE);
        });
        this.eventAggregateListener = eventAggregateListener;
        this.dvhRestConsumer = dvhRestConsumer;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        if (eventAggregateListener.initloadComplete() && dvhRestConsumer.initloadComplete()) {
            return ReadinessState.ACCEPTING_TRAFFIC;
        } else {
            return ReadinessState.REFUSING_TRAFFIC;
        }
    }

}