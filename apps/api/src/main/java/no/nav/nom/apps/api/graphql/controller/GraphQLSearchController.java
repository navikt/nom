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

import java.util.List;

import static java.util.Objects.isNull;

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
    public List<Object> search(@Argument("term") String term, @Argument("ressursFilter") RessursSearchFilter ressursFilter, @Argument("orgEnhetFilter") OrgEnhetSearchFilter orgEnhetFilter, DataFetchingEnvironment environment) {
        counter.count(environment);
        var ressurs = isNull(ressursFilter) ? RessursSearchFilter.alle() : ressursFilter;
        var orgenhet = isNull(orgEnhetFilter) ? OrgEnhetSearchFilter.alle() : orgEnhetFilter;
        return searchResolver.search(term, ressurs, orgenhet);
    }

    @QueryMapping("searchRessurs")
    public List<Ressurs> searchRessurs(@Argument("term") String term, @Argument("filter") RessursSearchFilter filter, DataFetchingEnvironment environment) {
        counter.count(environment);
        var searchFilter = isNull(filter) ? RessursSearchFilter.alle() : filter;
        return searchResolver.searchRessurs(term, searchFilter);
    }

    @QueryMapping("searchOrgEnhet")
    public List<OrgEnhet> searchOrgEnhet(@Argument("term") String term, @Argument("filter") OrgEnhetSearchFilter filter, DataFetchingEnvironment environment) {
        counter.count(environment);
        var searchFilter = isNull(filter) ? OrgEnhetSearchFilter.alle() : filter;
        return searchResolver.searchOrgenhet(term, searchFilter);
    }

}
