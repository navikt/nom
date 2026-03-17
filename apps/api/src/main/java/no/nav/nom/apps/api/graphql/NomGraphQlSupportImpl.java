package no.nav.nom.apps.api.graphql;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.libs.gql.NomGraphqlSupportInterface;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NomGraphQlSupportImpl implements NomGraphqlSupportInterface {

    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final RessursPropertyResolver ressursPropertyResolver;
    private final GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final OrganiseringStateRepository organiseringStateRepository;

    @Value("${app.orgenhet.nav}")
    private String navToppnodeOrgEnhetId;
    @Value("${app.orgenhet.vikafossenId}")
    private String vikafossenOrgEnhetId;
    @Value("${app.orgenhet.helfo}")
    private String helfoOrgEnhetId;
    @Value("${app.orgenhet.skatt}")
    private String skattOrgEnhetId;
    @Value("${app.orgenhet.riksrev}")
    private String riksrevOrgEnhetId;

    NomGraphQlSupportImpl(
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        RessursPropertyResolver ressursPropertyResolver,
        GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter,
        DvhStartupLoadMonitor dvhStartupLoadMonitor, OrganiseringStateRepository organiseringStateRepository
    ) {
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.ressursPropertyResolver = ressursPropertyResolver;
        this.graphQLFieldConsumptionCounter = graphQLFieldConsumptionCounter;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
        this.organiseringStateRepository = organiseringStateRepository;
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

    @Override
    public String getNavToppnodeOrgEnhetId() {
        return navToppnodeOrgEnhetId;
    }

    @Override
    public String getOverEnhetIdForVikafossenOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(vikafossenOrgEnhetId);
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return vikafossenOrgEnhetId;
    }

    @Override
    public String getOverEnhetIdForHelfoOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(helfoOrgEnhetId);
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return helfoOrgEnhetId;
    }

    @Override
    public String getOverEnhetIdForSkattOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(skattOrgEnhetId);
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return skattOrgEnhetId;
    }

    @Override
    public String getOverEnhetIdForRiksrevOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(riksrevOrgEnhetId);
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return riksrevOrgEnhetId;
    }
}
