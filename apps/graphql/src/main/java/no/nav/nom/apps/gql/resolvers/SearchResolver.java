package no.nav.nom.apps.gql.resolvers;

import no.nav.nom.apps.gql.projections.OrgEnhetSearchFilter;
import no.nav.nom.apps.gql.projections.RessursSearchFilter;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchResolver {
    public List<Object> search(String term, RessursSearchFilter ressurs, OrgEnhetSearchFilter orgenhet) {
        throw new RuntimeException("Not implemented"); // todo
    }

    public List<Ressurs> searchRessurs(String term, RessursSearchFilter searchFilter) {
        throw new RuntimeException("Not implemented"); // todo
    }

    public List<OrgEnhet> searchOrgenhet(String term, OrgEnhetSearchFilter searchFilter) {
        throw new RuntimeException("Not implemented"); // todo
    }
}
