package no.nav.nom.apps.gql.resolvers;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

import java.util.List;
import java.util.Map;

public interface RessursPropertyResolverInterface {
    Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser);
}
