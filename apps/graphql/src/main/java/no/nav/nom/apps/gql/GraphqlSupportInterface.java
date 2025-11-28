package no.nav.nom.apps.gql;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface GraphqlSupportInterface {

    String remedyEnhetId(String nomId);

    Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser);

    void count(DataFetchingEnvironment dataFetchingEnvironment);

    boolean isWaitingToIndexLucene();

    static GraphqlSupportInterface dummyImplementation(){
        return new GraphqlSupportInterface() {
            @Override
            public String remedyEnhetId(String nomId) {
                return "remedy-"+nomId;
            }

            @Override
            public Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser) {
                return ressurser.stream().collect(
                    Collectors.toMap(
                        (ressurs) -> ressurs,
                        (ressurs) -> DataFetcherResult.<String>newResult().data(ressurs.getNavident()+"@dummy.mail").build())
                );
            }

            @Override
            public void count(DataFetchingEnvironment dataFetchingEnvironment) {
                // do nothing
            }

            @Override
            public boolean isWaitingToIndexLucene() {
                return false;
            }
        };
    }


}
