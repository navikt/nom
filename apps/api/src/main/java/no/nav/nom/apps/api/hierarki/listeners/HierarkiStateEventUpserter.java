package no.nav.nom.apps.api.hierarki.listeners;

import no.nav.nom.apps.api.hierarki.HierarkiService;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Todo, hook up with Nom listeners properly
 */
@Component
public class HierarkiStateEventUpserter extends HierarkiService.Upserts {

//    final AbstractNomStateListener.Visitor<RessursState> ressursListener;
//    final AbstractNomStateListener.Visitor<OrganiseringState> organiseringListener;
//    final AbstractNomStateListener.Visitor<OrgEnhetState> orgenhetListener;
//    final AbstractNomStateListener.Visitor<OrgEnhetLederState> lederListener;
//    final AbstractNomStateListener.Visitor<OrgtilknytningState> orgtilknytningListener;
//    final AbstractNomStateListener.Visitor<LederTilgangState> delegertLederListener;


    public HierarkiStateEventUpserter(HierarkiService hierarkiService) {
        super();
//
//        this.ressursListener = createVisitor("HierarkiStateEventListener.ressurs", res -> {});
//        this.orgenhetListener = createVisitor("HierarkiStateEventListener.orgenhet", hierarkiService::upsertOrgenhet);
//        this.organiseringListener = createVisitor("HierarkiStateEventListener.organisering", hierarkiService::upsertOrganisering);
//        this.orgtilknytningListener = createVisitor("HierarkiStateEventListener.orgtilknytning", hierarkiService::upsertOrgtilknytning);
//        this.lederListener = createVisitor("HierarkiStateEventListener.leder", hierarkiService::upsertOrgenhetLeder);
//        this.delegertLederListener = createVisitor("HierarkiStateEventListener.delegertLeder", hierarkiService::upsertLederTilgang);

    }

    private <T> AbstractNomStateListener.Visitor<T> createVisitor(String name, Consumer<T> consumer){
        return new  AbstractNomStateListener.Visitor<T>(){
            @Override
            public String visitorName() {
                return name;
            }
            @Override
            public void visit(String key, T stateObject) {
                consumer.accept(stateObject);
            }
        };
    }

}
