package no.nav.nom.apps.api.lucenesearch;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Component
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
