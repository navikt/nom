package no.nav.nom.apps.sfintegrasjon.common;

import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.application.AvailabilityStateHealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

@Component
public class CustomCheckHealthIndicator extends AvailabilityStateHealthIndicator {
    private final NomStateRessursListener nomStateRessursListener;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;
    private final NomStateOrganiseringListener nomStateOrganiseringListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final NomStateMasteredRessursListener nomStateMasteredRessursListener;
    private final NomStateDelegertLederListener nomStateDelegertLederListener;
    private final NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener;

    public CustomCheckHealthIndicator(
        ApplicationAvailability availability,
        NomStateRessursListener nomStateRessursListener,
        NomStateOrgEnhetListener nomStateOrgEnhetListener,
        NomStateOrganiseringListener nomStateOrganiseringListener,
        NomStateOrgtilknytningListener nomStateOrgtilknytningListener,
        NomStateMasteredRessursListener nomStateMasteredRessursListener,
        NomStateDelegertLederListener nomStateDelegertLederListener,
        NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener
    ) {
        super(availability, ReadinessState.class, (statusMappings) -> {
            statusMappings.add(ReadinessState.ACCEPTING_TRAFFIC, Status.UP);
            statusMappings.add(ReadinessState.REFUSING_TRAFFIC, Status.OUT_OF_SERVICE);
        });
        this.nomStateRessursListener = nomStateRessursListener;
        this.nomStateOrgEnhetListener = nomStateOrgEnhetListener;
        this.nomStateOrganiseringListener = nomStateOrganiseringListener;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
        this.nomStateMasteredRessursListener = nomStateMasteredRessursListener;
        this.nomStateDelegertLederListener = nomStateDelegertLederListener;
        this.nomStateOrgEnhetLederListener = nomStateOrgEnhetLederListener;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        if (nomStateRessursListener.hasReadToEndOnStartup() &&
            nomStateOrgEnhetListener.hasReadToEndOnStartup() &&
            nomStateOrganiseringListener.hasReadToEndOnStartup() &&
            nomStateOrgtilknytningListener.hasReadToEndOnStartup() &&
            nomStateMasteredRessursListener.hasReadToEndOnStartup() &&
            nomStateDelegertLederListener.hasReadToEndOnStartup() &&
            nomStateOrgEnhetLederListener.hasReadToEndOnStartup()
        ) {
            return ReadinessState.ACCEPTING_TRAFFIC;
        } else {
            return ReadinessState.REFUSING_TRAFFIC;
        }
    }
}
