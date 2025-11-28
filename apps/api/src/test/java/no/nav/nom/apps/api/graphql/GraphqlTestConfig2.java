package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.gql.GraphqlSupportInterface;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;


@Import({WebClientAutoConfiguration.class, SecurityAutoConfiguration.class })
public class GraphqlTestConfig2 {

    @Bean
    public OrganiseringStateRepositoryImpl organiseringStateRepository() {
        return new OrganiseringStateRepositoryImpl();
    }

    @Bean
    public OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository() {
        return new OrgtilknytningStateRepositoryImpl();
    }


    @Bean public OrgEnhetLederStateRepositoryImpl orgEnhetLederRepository(){
        return new OrgEnhetLederStateRepositoryImpl();
    };

    @Bean
    public RessursStateRepositoryImpl ressursStateRepository(){
        return new RessursStateRepositoryImpl();
    }

    @Bean
    public OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository(){
        return new OrgEnhetLederStateRepositoryImpl();
    }

    @Bean
    public OrgEnhetStateRepositoryImpl orgEnhetStateRepository(){
        return new OrgEnhetStateRepositoryImpl();
    }


    @Bean
    public TestTokensGenerator testTokensGenerator(){
        return  new TestTokensGenerator();
    }

    @Bean
    public GraphQLTestUtil graphQLTestUtil() {
        return new GraphQLTestUtil();
    }

    @Bean
    public GraphqlSupportInterface graphqlSupportInterface(){
        return GraphqlSupportInterface.dummyImplementation();
    }

    @Bean
    public SecurityUtil securityUtil(Environment environment) {
        return new SecurityUtil(environment);
    }

}
