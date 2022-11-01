package no.nav.nom.apps.api.graphql.controller;

import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetKoblingResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrganiseringResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch.EMPTY_SEARCH;

@Controller
@PreAuthorize("@securityUtil.checkAuth(authentication)")
public class GraphQLOrgEnhetController {

    private final OrgEnhetResolver orgEnhetResolver;
    private final OrgEnhetKoblingResolver koblingResolver;
    private final OrganiseringResolver organiseringResolver;
    private final LederResolver lederResolver;
    private final GraphQLFieldConsumptionCounter counter;

    public GraphQLOrgEnhetController(
            OrgEnhetResolver orgEnhetResolver,
            OrgEnhetKoblingResolver koblingResolver,
            OrganiseringResolver organiseringResolver,
            @Qualifier("OrgEnhetsLeder") LederResolver lederResolver,
            GraphQLFieldConsumptionCounter counter
    ) {
        this.orgEnhetResolver = orgEnhetResolver;
        this.koblingResolver = koblingResolver;
        this.organiseringResolver = organiseringResolver;
        this.lederResolver = lederResolver;
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

    @SchemaMapping(typeName = "OrgEnhet", field = "organiseringer")
    public List<Organisering> organisering(OrgEnhet orgEnhet, @Argument("retning") OrganiseringRetning retning) {
        return organiseringResolver.organiseringer(orgEnhet, retning);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "leder")
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet) {
        return lederResolver.leder(orgEnhet);
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
