package no.nav.nom.apps.api.lucenesearch;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.lucenesearch.LuceneService.UNLEASH_USE_NEW_RESSURS_STATE_LISTENER;

@Component
public class RessursStateVisitor implements NomStateRessursListener.Visitor{
    private final LuceneService luceneService;
    private final NomStateRessursListener nomStateRessursListener;
    private final Unleash unleash;

    public RessursStateVisitor(LuceneService luceneService, NomStateRessursListener nomStateRessursListener, Unleash unleash) {
        this.luceneService = luceneService;
        this.nomStateRessursListener = nomStateRessursListener;
        this.unleash = unleash;

        nomStateRessursListener.register(this);
    }

    @Override
    public String visitorName() {
        return "lucenesearch.RessursStateVisitor";
    }

    @Override
    public void visit(RessursState ressursState) {
        if (!isNull(ressursState) && unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_LISTENER)) {
            luceneService.invalidateRessurs(ressursState.navident());
        }
    }
}
