package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.apps.gql.GraphQlAutoConfiguration;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.test.context.ContextConfiguration;

@GraphQlTest
@ContextConfiguration(
    classes = {
        GraphQlSupportImpl.class,
        GraphQlControllerForTest.class,
        GraphQlAutoConfiguration.class,
        GraphQlTestBase.TestBeans.class
    },
    initializers = MockOAuth2ServerInitializer.class
)
abstract public class GraphQlTestBase implements ApplicationContextAware {

    protected OrgEnhetStateRepository orgEnhetStateRepository;
    protected OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    protected RessursStateRepository ressursStateRepository;
    protected OrgtilknytningStateRepository orgtilknytningStateRepository;
    protected OrganiseringStateRepository organiseringStateRepository;

    protected RemedyEnhetIdResolver remedyEnhetIdResolver;
    protected  RessursPropertyResolver ressursPropertyResolver;
    protected GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;
    protected DvhStartupLoadMonitor dvhStartupLoadMonitor;

    protected AdClient adClient;
    protected GraphQLTestUtil gqlUtil;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.orgEnhetStateRepository = applicationContext.getBean(OrgEnhetStateRepository.class);
        this.orgEnhetLederStateRepository = applicationContext.getBean(OrgEnhetLederStateRepository.class);
        this.ressursStateRepository = applicationContext.getBean(RessursStateRepository.class);
        this.orgtilknytningStateRepository = applicationContext.getBean(OrgtilknytningStateRepository.class);
        this.organiseringStateRepository = applicationContext.getBean(OrganiseringStateRepository.class);
        this.remedyEnhetIdResolver = applicationContext.getBean(RemedyEnhetIdResolver.class);
        this.ressursPropertyResolver = applicationContext.getBean(RessursPropertyResolver.class);
        this.graphQLFieldConsumptionCounter = applicationContext.getBean(GraphQLFieldConsumptionCounter.class);
        this.dvhStartupLoadMonitor = applicationContext.getBean(DvhStartupLoadMonitor.class);
        this.gqlUtil = applicationContext.getBean(GraphQLTestUtil.class);
        this.adClient = applicationContext.getBean(AdClient.class);

    }

    @Import({WebClientAutoConfiguration.class, SecurityAutoConfiguration.class, TestTokensGenerator.class })
    static class TestBeans {

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
        public AdClient adClient(){
            return Mockito.mock(AdClient.class);
        }

        @Bean
        protected final RessursPropertyResolver ressursPropertyResolver(Environment env, AdClient adClient, RessursStateRepository ressursStateRepository){
            return Mockito.spy(new RessursPropertyResolver(new ActiveDirectoryRessursBatchLoader(adClient),env,ressursStateRepository));
//            return Mockito.mock(RessursPropertyResolver.class);
        };
        @Bean public GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter(){
            return Mockito.mock(GraphQLFieldConsumptionCounter.class);
        }
        @Bean public DvhStartupLoadMonitor dvhStartupLoadMonitor(){
            return Mockito.mock(DvhStartupLoadMonitor.class);
        }

        @Bean
        public GraphQLTestUtil graphQLTestUtil(ExecutionGraphQlServiceTester tester, ResourceLoader resourceLoader, TestTokensGenerator testTokensGenerator) {
            return new GraphQLTestUtil(tester,resourceLoader,testTokensGenerator);
        }

        @Bean
        public SecurityUtil securityUtil(Environment environment) {
            return new SecurityUtil(environment);
        }


    }
}
