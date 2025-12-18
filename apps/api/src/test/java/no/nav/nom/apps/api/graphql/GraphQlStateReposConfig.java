package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.ResourceLoader;

public class GraphQlStateReposConfig {

    @Bean
    public OrgEnhetStateRepositoryImpl orgEnhetStateRepository(){
        return Mockito.spy(new OrgEnhetStateRepositoryImpl());
    }

    @Bean
    public OrganiseringStateRepositoryImpl organiseringStateRepository() {
        return Mockito.spy(new OrganiseringStateRepositoryImpl());
    }

    @Bean
    public OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository() {
        return Mockito.spy(new OrgtilknytningStateRepositoryImpl());
    }

    @Bean
    public RessursStateRepositoryImpl ressursStateRepository(){
        return Mockito.spy(new RessursStateRepositoryImpl());
    }

    @Bean
    public OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository(){
        return Mockito.spy(new OrgEnhetLederStateRepositoryImpl());
    }

    @Bean
    public RemedyEnhetIdResolver remedyEnhetIdResolver(){
        return Mockito.mock(RemedyEnhetIdResolver.class);
    }

    @Bean
    public ResourceLoader resourceLoader() {
        return new ClassRelativeResourceLoader(GraphQlTestBase.class);
    }

    @Bean
    public AdClient adClient(){
        return Mockito.mock(AdClient.class);
    }

    @Bean
    protected final RessursPropertyResolver ressursPropertyResolver(Environment env, AdClient adClient, RessursStateRepository ressursStateRepository){
        return Mockito.spy(new RessursPropertyResolver(new ActiveDirectoryRessursBatchLoader(adClient,null),env,ressursStateRepository));
    };

    @Bean public GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter(){
        return Mockito.mock(GraphQLFieldConsumptionCounter.class);
    }

    @Bean public DvhStartupLoadMonitor dvhStartupLoadMonitor(){
        var out = Mockito.mock(DvhStartupLoadMonitor.class);
        Mockito.when(out.hasCompleted()).thenReturn(true);
        return out;
    }

    @Bean
    public SecurityUtil securityUtil(Environment environment) {
        return new SecurityUtil(environment);
    }

}
