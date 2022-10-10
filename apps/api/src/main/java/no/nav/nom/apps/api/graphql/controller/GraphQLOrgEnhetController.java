package no.nav.nom.apps.api.graphql.controller;

import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetKoblingResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrganiseringResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
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

import static no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch.EMPTY_SEARCH;

@Controller
public class GraphQLOrgEnhetController {

    private final OrgEnhetResolver orgEnhetResolver;
    private final OrgEnhetKoblingResolver koblingResolver;
    private final OrganiseringResolver organiseringResolver;
    private final GraphQLFieldConsumptionCounter counter;

    public GraphQLOrgEnhetController(
            OrgEnhetResolver orgEnhetResolver,
            OrgEnhetKoblingResolver koblingResolver,
            OrganiseringResolver organiseringResolver,
            GraphQLFieldConsumptionCounter counter
    ) {
        this.orgEnhetResolver = orgEnhetResolver;
        this.koblingResolver = koblingResolver;
        this.organiseringResolver = organiseringResolver;
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
    public List<OrgEnhetResult> organisasjonsenheter(@Argument("where") Optional<OrgEnheterSearch> orgEnheterSearch, DataFetchingEnvironment environment) {
        return orgEnheter(orgEnheterSearch, environment);
    }

    @QueryMapping("orgEnheter")
    public List<OrgEnhetResult> orgEnheter(@Argument("where") Optional<OrgEnheterSearch> orgEnheterSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnheter(orgEnheterSearch.orElse(EMPTY_SEARCH));
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "organiseringer")
    public List<Organisering> organisering(OrgEnhet orgEnhet, @Argument("retning") OrganiseringRetning retning) {
        return organiseringResolver.organiseringer(orgEnhet, retning);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "leder")
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet) {
        return Collections.emptyList();
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "koblinger")
    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        return koblingResolver.koblinger(orgEnhet);
    }

    @SchemaMapping(typeName = "Organisering", field = "organisasjonsenhet")
    public OrgEnhet organisasjonsenhet(Organisering organisering) {
        return orgEnhet(organisering);
    }
    @SchemaMapping(typeName = "Organisering", field = "orgEnhet")
    public OrgEnhet orgEnhet(Organisering organisering) {
        return orgEnhetResolver.orgEnhet(organisering);
    }
}
