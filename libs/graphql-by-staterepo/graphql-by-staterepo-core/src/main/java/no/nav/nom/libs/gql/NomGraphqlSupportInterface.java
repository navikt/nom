package no.nav.nom.libs.gql;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface NomGraphqlSupportInterface {

    String remedyEnhetId(String nomId);

    Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser);

    String getNavToppnodeOrgEnhetId();

    String getOverEnhetIdForVikafossenOrgEnhet();

    String getOverEnhetIdForHelfoOrgEnhet();

    String getOverEnhetIdForSkattOrgEnhet();

    String getOverEnhetIdForRiksrevOrgEnhet();

    void count(DataFetchingEnvironment dataFetchingEnvironment);

    boolean isWaitingToIndexLucene();

    static NomGraphqlSupportInterface dummyImplementation(){
        return new NomGraphqlSupportInterface() {
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
            public String getNavToppnodeOrgEnhetId() {
                return "DUMMY";
            }

            @Override
            public String getOverEnhetIdForVikafossenOrgEnhet() {
                return "DUMMY";
            }

            @Override
            public String getOverEnhetIdForHelfoOrgEnhet() {
                return "DUMMY";
            }

            @Override
            public String getOverEnhetIdForSkattOrgEnhet() {
                return "DUMMY";
            }

            @Override
            public String getOverEnhetIdForRiksrevOrgEnhet() {
                return "DUMMY";
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
