package no.nav.nom.apps.api.graphql.controller;

import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.search.SearchResolver;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;

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
    public List<Object> searchRessurs(@Argument("term") String term, DataFetchingEnvironment environment) {
        counter.count(environment);
        return Collections.emptyList();
    }

}
