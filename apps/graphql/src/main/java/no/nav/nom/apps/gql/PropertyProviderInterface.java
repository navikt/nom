package no.nav.nom.apps.gql;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

import java.util.List;
import java.util.Map;

public interface PropertyProviderInterface {

    String remedyEnhetId(String nomId);

    Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser);

    void count(DataFetchingEnvironment dataFetchingEnvironment);



}
