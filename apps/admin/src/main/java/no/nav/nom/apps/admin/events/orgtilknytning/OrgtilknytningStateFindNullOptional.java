package no.nav.nom.apps.admin.events.orgtilknytning;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrgtilknytningStateFindNullOptional implements AbstractNomStateListener.Visitor<OrgtilknytningState> {

    public OrgtilknytningStateFindNullOptional(NomStateOrgtilknytningListener orgtilknytningListener) {
        orgtilknytningListener.register(this);
    }

    @Override
    public String visitorName() {
        return "Visitor for å prøve å finne Orgtilknytning som har null og ikke Optional.Empty()";
    }

    @Override
    public void visit(String key, OrgtilknytningState stateObject) {
        if (stateObject != null && stateObject.masseflyttetFraFid() == null) {
            log.error("Orgtilknytning FID {} har null i masseFlyttetFraFid", key);
        }
    }
}
