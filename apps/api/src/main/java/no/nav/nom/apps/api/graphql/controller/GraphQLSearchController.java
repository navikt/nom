package no.nav.nom.apps.api.graphql.controller;

import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearchFilter;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearchFilter;
import no.nav.nom.apps.api.graphql.search.SearchResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@PreAuthorize("@securityUtil.checkAuth(authentication)")
public class GraphQLSearchController {

    private final SearchResolver searchResolver;
    private final GraphQLFieldConsumptionCounter counter;

    public GraphQLSearchController(SearchResolver searchResolver, GraphQLFieldConsumptionCounter counter) {
        this.searchResolver = searchResolver;
        this.counter = counter;
    }

    @QueryMapping("search")
    public List<Object> search(@Argument("term") String term, DataFetchingEnvironment environment) {
        counter.count(environment);
        return searchResolver.search(term);
    }

    @QueryMapping("searchRessurs")
    public List<Ressurs> searchRessurs(@Argument("term") String term, @Argument("filter") Optional<RessursSearchFilter> filter, DataFetchingEnvironment environment) {
        counter.count(environment);
        return searchResolver.searchRessurs(term,filter.orElseGet(RessursSearchFilter::alle));
    }

    @QueryMapping("searchOrgEnhet")
    public List<OrgEnhet> searchOrgEnhet(@Argument("term") String term, @Argument("filter") Optional<OrgEnhetSearchFilter> filter, DataFetchingEnvironment environment) {
        counter.count(environment);
        return searchResolver.searchOrgenhet(term,filter.orElseGet(OrgEnhetSearchFilter::alle));
    }

}
