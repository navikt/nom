package no.nav.nom.apps.api.graphql.controller;

import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgtilknytningResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrganiseringResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.OrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
@PreAuthorize("@securityUtil.checkAuth(authentication)")
public class GraphQLOrgEnhetController {

    private final OrgEnhetResolver orgEnhetResolver;
    private final OrgtilknytningResolver orgtilknytningResolver;
    private final OrganiseringResolver organiseringResolver;
    private final LederResolver lederResolver;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final GraphQLFieldConsumptionCounter counter;

    public GraphQLOrgEnhetController(
        OrgEnhetResolver orgEnhetResolver,
        OrgtilknytningResolver orgtilknytningResolver,
        OrganiseringResolver organiseringResolver,
        @Qualifier("OrgEnhetsLeder") LederResolver lederResolver,
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        GraphQLFieldConsumptionCounter counter
    ) {
        this.orgEnhetResolver = orgEnhetResolver;
        this.orgtilknytningResolver = orgtilknytningResolver;
        this.organiseringResolver = organiseringResolver;
        this.lederResolver = lederResolver;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.counter = counter;
    }

    @Deprecated(forRemoval = true) // NB! ikke fjern umiddelbart. Counter manglet - lagt inn 10.09
    @QueryMapping("organisasjonsenhet")
    public Optional<OrgEnhet> organisasjonsenhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhet(orgEnhetSearch, environment);
    }

    @QueryMapping("orgEnhet")
    public Optional<OrgEnhet> orgEnhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnhet(orgEnhetSearch);
    }

    @Deprecated(forRemoval = true)  // NB! ikke fjern umiddelbart. Counter manglet - lagt inn 10.09
    @QueryMapping("organisasjonsenheter")
    public List<OrgEnhetResult> organisasjonsenheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnheter(orgEnheterSearch, environment);
    }

    @QueryMapping("orgEnheter")
    public List<OrgEnhetResult> orgEnheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnheter(orgEnheterSearch);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "organiseringer")
    public List<Organisering> organisering(OrgEnhet orgEnhet, @Argument("retning") OrganiseringRetning retning, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg) {
        return organiseringResolver.organiseringer(orgEnhet, retning, periodiskUtvalg);
    }

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "leder")
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet) {
        return lederResolver.leder(orgEnhet, PeriodiskUtvalgArg.AKTIVE);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "ledere")
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg) {
        return lederResolver.leder(orgEnhet, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "remedyEnhetId")
    public String remedyEnhetId(OrgEnhet orgEnhet) {
        return remedyEnhetIdResolver.remedyEnhetId(orgEnhet);
    }

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "koblinger")
    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        return orgtilknytningResolver.koblinger(orgEnhet);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "orgTilknytninger")
    public List<OrgTilknytning> orgTilknytninger(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg) {
        return orgtilknytningResolver.orgtilknytninger(orgEnhet, periodiskUtvalg);
    }

    @Deprecated
    @SchemaMapping(typeName = "Organisering", field = "organisasjonsenhet")
    public OrgEnhet organisasjonsenhet(Organisering organisering) {
        return orgEnhet(organisering);
    }

    @SchemaMapping(typeName = "Organisering", field = "orgEnhet")
    public OrgEnhet orgEnhet(Organisering organisering) {
        return orgEnhetResolver.orgEnhet(organisering);
    }
}
