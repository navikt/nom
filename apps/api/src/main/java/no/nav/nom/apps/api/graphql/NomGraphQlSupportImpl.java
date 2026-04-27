package no.nav.nom.apps.api.graphql;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.libs.gql.NomGraphqlSupportInterface;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
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
    private final AppProperties.Orgenhet orgenhetProperties;


    NomGraphQlSupportImpl(
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        RessursPropertyResolver ressursPropertyResolver,
        GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter,
        DvhStartupLoadMonitor dvhStartupLoadMonitor, OrganiseringStateRepository organiseringStateRepository,
        AppProperties appProperties
    ) {
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.ressursPropertyResolver = ressursPropertyResolver;
        this.graphQLFieldConsumptionCounter = graphQLFieldConsumptionCounter;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgenhetProperties = appProperties.orgenhet();
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
        return orgenhetProperties.navId();
    }

    @Override
    public String getOverEnhetIdForVikafossenOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(orgenhetProperties.vikafossenId());
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return orgenhetProperties.vikafossenId();
    }

    @Override
    public String getOverEnhetIdForHelfoOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(orgenhetProperties.helfoId());
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return orgenhetProperties.helfoId();
    }

    @Override
    public String getOverEnhetIdForSkattOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(orgenhetProperties.skattId());
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return orgenhetProperties.skattId();
    }

    @Override
    public String getOverEnhetIdForRiksrevOrgEnhet() {
        var aktivOrganisering = organiseringStateRepository.findActiveById(orgenhetProperties.riksrevisjonenId());
        if (aktivOrganisering.isPresent()) {
            return aktivOrganisering.get().idOver();
        }
        return orgenhetProperties.riksrevisjonenId();
    }
}
