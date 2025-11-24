package no.nav.nom.libs.gql;

import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.libs.gql.exception.NotRelevantForIntegrationTestingException;
import no.nav.nom.libs.gql.projections.*;
import no.nav.nom.contract.nom.api.graphql.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NomGraphQlControllerInterface {
    static GraphQLError handleNotRelevantQueryException(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof NotRelevantForIntegrationTestingException) {
            return ErrorCode.DISABLED_FEATURE.construct(environment, "Ikke relevant query for integrasjonstesting");
        }
        return null;
    }

    @QueryMapping("orgEnhet")
    Optional<OrgEnhet> orgEnhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment);

    @QueryMapping("orgEnheter")
    List<OrgEnhetResult> orgEnheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment);

    @SchemaMapping(typeName = "OrgEnhet", field = "organiseringer")
    List<Organisering> organisering(
        OrgEnhet orgEnhet,
        @Argument("retning") OrganiseringRetning retning,
        @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg,
        DataFetchingEnvironment environment
    );

    @SchemaMapping(typeName = "OrgEnhet", field = "ledere")
    List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment);

    @SchemaMapping(typeName = "OrgEnhet", field = "remedyEnhetId")
    String remedyEnhetId(OrgEnhet orgEnhet, DataFetchingEnvironment environment);

    @SchemaMapping(typeName = "OrgEnhet", field = "orgTilknytninger")
    List<OrgTilknytning> orgTilknytninger(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment);

    @SchemaMapping(typeName = "Organisering", field = "orgEnhet")
    OrgEnhet orgEnhet(Organisering organisering, DataFetchingEnvironment environment);

    @QueryMapping("ressurs")
    Optional<Ressurs> ressurs(@Argument("where") RessursSearch search, DataFetchingEnvironment environment);

    @QueryMapping("ressurser")
    List<RessursResult> ressurser(@Argument("where") RessurserSearch search, DataFetchingEnvironment environment);

    @QueryMapping("ressurserWithMetadata")
    List<RessursWithMetadataResult> ressurserWithMetadata(DataFetchingEnvironment environment);

    @SchemaMapping(typeName = "Ressurs", field = "orgTilknytninger")
    List<RessursOrgTilknytning> orgTilknytninger(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment);

    @SchemaMapping(typeName = "Ressurs", field = "lederFor")
    List<LederOrgEnhet> lederFor(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment);

    @SchemaMapping(typeName = "Ressurs", field = "ledere")
    List<RessursLeder> ledere(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment);

    @BatchMapping(typeName = "Ressurs", field = "epost")
    Mono<Map<Ressurs, DataFetcherResult<String>>> epost(List<Ressurs> ressurser);

    @QueryMapping("search")
    List<Object> search(
        @Argument("term") String term,
        @Argument("ressursFilter") RessursSearchFilter ressursFilter,
        @Argument("orgEnhetFilter") OrgEnhetSearchFilter orgEnhetFilter,
        DataFetchingEnvironment environment
    );

    @QueryMapping("searchRessurs")
    List<Ressurs> searchRessurs(@Argument("term") String term, @Argument("filter") RessursSearchFilter filter, DataFetchingEnvironment environment);

    @QueryMapping("searchOrgEnhet")
    List<OrgEnhet> searchOrgEnhet(@Argument("term") String term, @Argument("filter") OrgEnhetSearchFilter filter, DataFetchingEnvironment environment);

    @Deprecated(forRemoval = true) // NB! ikke fjern umiddelbart. Counter manglet - lagt inn 10.09
    @QueryMapping("organisasjonsenhet")
    Optional<OrgEnhet> organisasjonsenhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment);

    @Deprecated(forRemoval = true)  // NB! ikke fjern umiddelbart. Counter manglet - lagt inn 10.09
    @QueryMapping("organisasjonsenheter")
    List<OrgEnhetResult> organisasjonsenheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment);

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "leder")
    List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, DataFetchingEnvironment environment);

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "koblinger")
    List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet, DataFetchingEnvironment environment);

    @Deprecated
    @SchemaMapping(typeName = "Ressurs", field = "orgTilknytning")
    List<RessursOrgTilknytning> orgTilknytning(Ressurs ressurs, DataFetchingEnvironment environment);
}
