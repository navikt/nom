package no.nav.nom.apps.api.graphql;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.libs.gql.NomGraphqlSupportInterface;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NomGraphQlSupportImpl implements NomGraphqlSupportInterface {

    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final RessursPropertyResolver ressursPropertyResolver;
    private final GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;

    NomGraphQlSupportImpl(
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        RessursPropertyResolver ressursPropertyResolver,
        GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter,
        DvhStartupLoadMonitor dvhStartupLoadMonitor
    ) {
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.ressursPropertyResolver = ressursPropertyResolver;
        this.graphQLFieldConsumptionCounter = graphQLFieldConsumptionCounter;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
    }

    @Override
    public String remedyEnhetId(String nomId) {
        return remedyEnhetIdResolver.remedyEnhetId(nomId);
    }

    @Override
    public Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser) {
        return ressursPropertyResolver.epost(ressurser);
    }

    @Override
    public void count(DataFetchingEnvironment dataFetchingEnvironment) {
        graphQLFieldConsumptionCounter.count(dataFetchingEnvironment);
    }

    @Override
    public boolean isWaitingToIndexLucene() {
        return !dvhStartupLoadMonitor.hasCompleted();
    }
}
