package no.nav.nom.apps.gql;

import no.nav.nom.apps.gql.lucenesearch.LuceneService;
import no.nav.nom.apps.gql.lucenesearch.OrgEnhetStateVisitor;
import no.nav.nom.apps.gql.lucenesearch.ReadyForIndexingInterface;
import no.nav.nom.apps.gql.lucenesearch.RessursStateVisitor;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class GraphQlAutoConfiguration {

    private final LuceneService luceneService;
    private final GraphQlController graphQlController;
    private final OrgEnhetStateVisitor orgEnhetStateVisitor;
    private final RessursStateVisitor ressursStateVisitor;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;
    private final NomStateRessursListener nomStateRessursListener;


    public  GraphQlAutoConfiguration(
                                     OrganiseringStateRepository organiseringStateRepository,
                                     OrgtilknytningStateRepository orgtilknytningStateRepository,
                                     RessursStateRepository ressursStateRepository,
                                     OrgEnhetLederStateRepository orgEnhetLederStateRepository,
                                     OrgEnhetStateRepository orgEnhetStateRepository,
                                     ReadyForIndexingInterface readyForIndexing,
                                     FieldUsageCounterInterface fieldUsageCounter,
                                     RessursPropertyResolverInterface propertyResolver,
                                     RemedyIdResolverInterface remedyIdResolver,
                                     Environment environment,
                                     FieldUsageCounterInterface fieldUsageCounterInterface,
                                     NomStateOrgEnhetListener nomStateOrgEnhetListener,
                                     NomStateRessursListener nomStateRessursListener
    ) {
        this.luceneService = new LuceneService(ressursStateRepository, environment, orgEnhetStateRepository, readyForIndexing);
        this.graphQlController = new GraphQlController(
            new OrgEnhetResolver(orgEnhetStateRepository, new AgressoOrgenhetService(orgEnhetStateRepository)),
            new OrgtilknytningResolver(orgtilknytningStateRepository,ressursStateRepository,orgEnhetStateRepository),
            new OrganiseringResolver(organiseringStateRepository),
            new LederResolver(orgEnhetLederStateRepository,ressursStateRepository,orgEnhetStateRepository, orgtilknytningStateRepository, organiseringStateRepository),
            remedyIdResolver,
            new RessursResolver(ressursStateRepository),
            propertyResolver,
            new SearchResolver(luceneService,ressursStateRepository,orgEnhetStateRepository),
            fieldUsageCounter
        );
        this.orgEnhetStateVisitor = new OrgEnhetStateVisitor(luceneService,nomStateOrgEnhetListener);
        this.ressursStateVisitor = new RessursStateVisitor(luceneService, nomStateRessursListener);
        this.nomStateOrgEnhetListener = nomStateOrgEnhetListener;
        this.nomStateRessursListener = nomStateRessursListener;
    }

    @Bean
    public LuceneService getLuceneService() {
        return luceneService;
    }

    @Bean
    public GraphQlController getGraphQlController() {
        return graphQlController;
    }

    @Bean
    public OrgEnhetStateVisitor getOrgEnhetStateVisitor() {
        return orgEnhetStateVisitor;
    }

    @Bean
    public RessursStateVisitor getRessursStateVisitor() {
        return ressursStateVisitor;
    }

    @Bean
    public GraphQLConfig getGraphQLConfig() {
        return new GraphQLConfig();
    }
}
