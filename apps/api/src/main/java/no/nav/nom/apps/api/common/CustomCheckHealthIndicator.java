package no.nav.nom.apps.api.common;

import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateListener;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateListener;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import org.springframework.boot.actuate.availability.AvailabilityStateHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;

@Component
public class CustomCheckHealthIndicator extends AvailabilityStateHealthIndicator {

    private final RessursStateListener ressursStateListener;
    private final OrgEnhetStateListener orgEnhetStateListener;
    private final OrgtilknytningStateListener orgtilknytningStateListener;
    private final DvhRestConsumer dvhRestConsumer;
    private final LuceneService luceneService;

    public CustomCheckHealthIndicator(
        ApplicationAvailability availability,
        RessursStateListener ressursStateListener, OrgEnhetStateListener orgEnhetStateListener,
        OrgtilknytningStateListener orgtilknytningStateListener, DvhRestConsumer dvhRestConsumer, LuceneService luceneService
    ) {
        super(availability, ReadinessState.class, (statusMappings) -> {
            statusMappings.add(ReadinessState.ACCEPTING_TRAFFIC, Status.UP);
            statusMappings.add(ReadinessState.REFUSING_TRAFFIC, Status.OUT_OF_SERVICE);
        });
        this.ressursStateListener = ressursStateListener;
        this.orgEnhetStateListener = orgEnhetStateListener;
        this.orgtilknytningStateListener = orgtilknytningStateListener;
        this.dvhRestConsumer = dvhRestConsumer;
        this.luceneService = luceneService;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        if (orgEnhetStateListener.initloadComplete() && ressursStateListener.initloadComplete() &&
            dvhRestConsumer.initloadComplete() && luceneService.initLoadComplete() &&
            orgtilknytningStateListener.initloadComplete()) {
            return ReadinessState.ACCEPTING_TRAFFIC;
        } else {
            return ReadinessState.REFUSING_TRAFFIC;
        }
    }

}