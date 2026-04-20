package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.libs.gql.NomGraphQlAutoConfiguration;
import no.nav.nom.libs.gql.lucenesearch.LuceneService;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.graphql.test.autoconfigure.tester.GraphQlTesterAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

@ContextConfiguration(
    classes = {
        NomGraphQlSupportImpl.class,
        NomGraphQlControllerForTest.class,
        JacksonAutoConfiguration.class,
        NomGraphQlAutoConfiguration.class,
        GraphQlTestBase.TestBeans.class,
        GraphQlStateReposConfig.class,
        GraphQlTesterAutoConfiguration.class
    }
)
@EnableConfigurationProperties(AppProperties.class)
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
    protected LuceneService luceneService;

    protected ResourceLoader resourceLoader;


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
        this.adClient = applicationContext.getBean(AdClient.class);
        this.luceneService = applicationContext.getBean(LuceneService.class);
        this.resourceLoader = applicationContext.getBean(ResourceLoader.class);


        {
            var jsonMapper = applicationContext.getBean(JsonMapper.class);
            var executionGraphQlService = applicationContext.getBean(ExecutionGraphQlService.class);

            var executionGraphQLServiceTester = ExecutionGraphQlServiceTester.builder(executionGraphQlService)
                .encoder(new JacksonJsonEncoder(jsonMapper, MediaType.APPLICATION_GRAPHQL_RESPONSE, MediaType.APPLICATION_JSON))
                .decoder(new JacksonJsonDecoder(jsonMapper, MediaType.APPLICATION_JSON))
                .build();

            // todo, make this work
            //      var executionGraphQLServiceTesterFromApplicationContext = applicationContext.getBean(ExecutionGraphQlServiceTester.class);


           this.gqlUtil = new GraphQLTestUtil(executionGraphQLServiceTester,resourceLoader);
        }


    }


    static class TestBeans {





    }
}
