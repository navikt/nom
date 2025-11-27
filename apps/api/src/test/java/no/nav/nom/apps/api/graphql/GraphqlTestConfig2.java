package no.nav.nom.apps.api.graphql;

import graphql.execution.DataFetcherResult;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.gql.FieldUsageCounterInterface;
import no.nav.nom.apps.gql.RemedyIdResolverInterface;
import no.nav.nom.apps.gql.RessursPropertyResolverInterface;
import no.nav.nom.apps.gql.lucenesearch.ReadyForIndexingInterface;
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
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.stream.Collectors;

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
    public ReadyForIndexingInterface readyForIndexing(){
        return () -> true;
    }

    @Bean
    public FieldUsageCounterInterface fieldUsageCounter(){
        return (dataFetchingEnvironment) -> {};
    }

    @Bean
    public RessursPropertyResolverInterface propertyResolver() {
        return ressurser -> ressurser.stream().collect(
            Collectors.toMap(
                (ressurs) -> ressurs,
                (ressurs) -> DataFetcherResult.<String>newResult().data(ressurs.getNavident()+"@dummy.mail").build())
        );
    }

    @Bean
    public RemedyIdResolverInterface remedyIdResolver() {
        return (s) -> "remedy-"+s;
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
    public ReadyForIndexingInterface readyForIndexingInterface(){
        return () -> true;
    }

    @Bean
    public SecurityUtil securityUtil(Environment environment) {
        return new SecurityUtil(environment);
    }

}
