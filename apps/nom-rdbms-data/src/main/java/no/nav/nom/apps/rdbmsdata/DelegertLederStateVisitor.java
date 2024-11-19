package no.nav.nom.apps.rdbmsdata;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.rdbmsdata.repository.LederTilgang;
import no.nav.nom.apps.rdbmsdata.repository.LederTilgangRepository;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DelegertLederStateVisitor implements NomStateDelegertLederListener.Visitor<AuthorizationEvent>{

    private final DelegertLederStateRepository delegertLederStateRepository;
    private final LederTilgangRepository lederTilgangRepository;

    public DelegertLederStateVisitor(NomStateDelegertLederListener delegertLederListener,
                                     DelegertLederStateRepository delegertLederStateRepository,
                                     LederTilgangRepository lederTilgangRepository
    ) {
        delegertLederListener.register(this);
        this.delegertLederStateRepository = delegertLederStateRepository;
        this.lederTilgangRepository = lederTilgangRepository;
    }

    @Override
    public String visitorName() { return "rdbms_data_collector.DelegertLederStateVisitor"; }

    @Override
    public void visit(String key, AuthorizationEvent eventObject) {
        var delegering = delegertLederStateRepository.findByFid(eventObject.getFid());
        delegering.ifPresent(this::process);
    }

    private void process(LederTilgangState state) {
        lederTilgangRepository.save(LederTilgang.from(state));
    }
}
