package no.nav.nom.apps.admin.events.orgtilknytning;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrgtilknytningStateFindNullOptional implements AbstractNomStateListener.Visitor<OrgtilknytningState> {
    private final NomStateOrgtilknytningListener orgtilknytningListener;

    public OrgtilknytningStateFindNullOptional(NomStateOrgtilknytningListener orgtilknytningListener) {
        this.orgtilknytningListener = orgtilknytningListener;

        orgtilknytningListener.register(this);
    }

    @Override
    public String visitorName() {
        return "Visitor for å prøve å finne Orgtilknytning som har null og ikke Optional.Empty()";
    }

    @Override
    public void visit(String key, OrgtilknytningState stateObject) {
        if (stateObject.masseflyttetFraFid() == null) {
            log.error("Orgtilknytninf FID {} har null i masseFlyttetFraFid", key);
        }
    }
}
