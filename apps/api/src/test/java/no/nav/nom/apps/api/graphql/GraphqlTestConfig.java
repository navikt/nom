package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.gql.AbstractGraphQlController;
import no.nav.nom.apps.gql.GraphqlSupportInterface;
import no.nav.nom.apps.gql.lucenesearch.LuceneService;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;


@Import({WebClientAutoConfiguration.class, SecurityAutoConfiguration.class, TestTokensGenerator.class })
public class GraphqlTestConfig {

    private final OrganiseringStateRepositoryImpl organiseringStateRepository;
    private final OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository;
    private final OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository;
    private final RessursStateRepositoryImpl ressursStateRepository;
    private final OrgEnhetStateRepositoryImpl orgEnhetStateRepository;
    private final GraphqlSupportInterface supportInterface;

    public GraphqlTestConfig() {
        organiseringStateRepository = new OrganiseringStateRepositoryImpl();
        orgtilknytningStateRepository = new OrgtilknytningStateRepositoryImpl();
        orgEnhetLederStateRepository = new OrgEnhetLederStateRepositoryImpl();
        ressursStateRepository = new RessursStateRepositoryImpl();
        orgEnhetStateRepository = new OrgEnhetStateRepositoryImpl();
        supportInterface = GraphqlSupportInterface.dummyImplementation();
    }

    @Bean
    public OrganiseringStateRepositoryImpl organiseringStateRepository() {
        return organiseringStateRepository;
    }

    @Bean
    public OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository() {
        return orgtilknytningStateRepository;
    }


    @Bean
    public RessursStateRepositoryImpl ressursStateRepository(){
        return ressursStateRepository;
    }

    @Bean
    public OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository(){
        return orgEnhetLederStateRepository;
    }

    @Bean
    public OrgEnhetStateRepositoryImpl orgEnhetStateRepository(){
        return orgEnhetStateRepository;
    }


    @Bean
    public GraphQLTestUtil graphQLTestUtil(ExecutionGraphQlServiceTester tester, ResourceLoader resourceLoader) {
        return new GraphQLTestUtil(tester,resourceLoader);
    }

    @Bean
    public GraphqlSupportInterface graphqlSupportInterface(){
        return GraphqlSupportInterface.dummyImplementation();
    }

    @Bean
    public SecurityUtil securityUtil(Environment environment) {
        return new SecurityUtil(environment);
    }

    @Bean
    @Profile("GraphQLTest")
    public LuceneService luceneService(RessursStateRepository ressursStateRepository, Environment env, OrgEnhetStateRepository orgEnhetStateRepository) {
        return new LuceneService(ressursStateRepository,env,orgEnhetStateRepository,this.supportInterface);
    }

    @Bean
    @Profile("GraphQLTest4")
    public AbstractGraphQlController graphQlController(                                                       OrgEnhetResolver orgEnhetResolver,
                                                       OrgtilknytningResolver orgtilknytningResolver,
                                                       OrganiseringResolver organiseringResolver,
                                                       LederResolver lederResolver,
                                                       RessursResolver ressursResolver,
                                                       SearchResolver searchResolver

                                                       ) {
        return new AbstractGraphQlController(
            orgEnhetResolver,
            orgtilknytningResolver,
            organiseringResolver ,
            lederResolver ,
            ressursResolver ,
            searchResolver,
            this.supportInterface
        ) {

        };
    }
}
