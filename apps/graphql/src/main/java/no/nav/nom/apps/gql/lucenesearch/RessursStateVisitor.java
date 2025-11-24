package no.nav.nom.apps.gql.lucenesearch;

import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Component
@ConditionalOnBean(NomStateRessursListener.class)
public class RessursStateVisitor implements NomStateRessursListener.Visitor<RessursState>{
    private final LuceneService luceneService;

    public RessursStateVisitor(LuceneService luceneService, NomStateRessursListener nomStateRessursListener) {
        this.luceneService = luceneService;
        nomStateRessursListener.register(this);
    }

    @Override
    public String visitorName() {
        return "lucenesearch.RessursStateVisitor";
    }

    @Override
    public void visit(String key, RessursState ressursState) {
        if (!isNull(ressursState)) {
            luceneService.invalidateRessurs(ressursState.navident());
        }
    }
}
