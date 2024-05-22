package no.nav.nom.apps.api.lucenesearch;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.orgenhet.eventsourcing.state.ToggledOrgEnhetStateRepository.UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY;

@Component
public class OrgEnhetStateVisitor implements NomStateRessursListener.Visitor<OrgEnhetState>{
    private final LuceneService luceneService;
    private final Unleash unleash;

    public OrgEnhetStateVisitor(LuceneService luceneService, NomStateOrgEnhetListener nomStateOrgEnhetListener, Unleash unleash) {
        this.luceneService = luceneService;
        this.unleash = unleash;

        nomStateOrgEnhetListener.register(this);
    }

    @Override
    public String visitorName() {
        return "lucenesearch.OrgenhetStateVisitor";
    }

    @Override
    public void visit(String key, OrgEnhetState orgEnhetState) {
        if (!isNull(orgEnhetState) && unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            luceneService.invalidateOrgenhet(orgEnhetState.id());
        }
    }
}
