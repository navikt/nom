package no.nav.nom.apps.api.graphql.controller;

import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class GraphQLOrgEnhetController {

    private final OrgEnhetResolver orgEnhetResolver;
    private final GraphQLFieldConsumptionCounter counter;

    public GraphQLOrgEnhetController(
            OrgEnhetResolver orgEnhetResolver,
            GraphQLFieldConsumptionCounter counter
    ) {
        this.orgEnhetResolver = orgEnhetResolver;
        this.counter = counter;
    }

    @Deprecated(forRemoval = true)
    @QueryMapping("organisasjonsenhet")
    public Optional<OrgEnhet> organisasjonsenhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        return orgEnhet(orgEnhetSearch, environment);
    }

    @QueryMapping("orgEnhet")
    public Optional<OrgEnhet> orgEnhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnhet(orgEnhetSearch);
    }

    @Deprecated(forRemoval = true)
    @QueryMapping("organisasjonsenheter")
    public List<OrgEnhetResult> organisasjonsenheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        return orgEnheter(orgEnheterSearch, environment);
    }

    @QueryMapping("orgEnheter")
    public List<OrgEnhetResult> orgEnheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnheter(orgEnheterSearch);
    }

    // OrgEnhet schema resolver starts here
    @SchemaMapping(typeName = "OrgEnhet", field = "organisering")
    public List<Organisering> organisering(OrgEnhet orgEnhet, @Argument("retning") OrganiseringRetning retning) {
        return Collections.emptyList();
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "leder")
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet) {
        return Collections.emptyList();
    }


}
