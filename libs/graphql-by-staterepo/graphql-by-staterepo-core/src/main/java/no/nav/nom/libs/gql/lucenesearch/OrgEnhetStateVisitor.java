package no.nav.nom.libs.gql.lucenesearch;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Component
@ConditionalOnBean(NomStateOrgEnhetListener.class)
public class OrgEnhetStateVisitor implements NomStateOrgEnhetListener.Visitor<OrgEnhetState>{
    private final LuceneService luceneService;

    public OrgEnhetStateVisitor(LuceneService luceneService, NomStateOrgEnhetListener nomStateOrgEnhetListener) {
        this.luceneService = luceneService;

        nomStateOrgEnhetListener.register(this);
    }

    @Override
    public String visitorName() {
        return "lucenesearch.OrgenhetStateVisitor";
    }

    @Override
    public void visit(String key, OrgEnhetState orgEnhetState) {
        if (!isNull(orgEnhetState)) {
            luceneService.invalidateOrgenhet(orgEnhetState.id());
        }
    }
}
