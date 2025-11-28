package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.propertyresolvers.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.ressurs.api.v1.dvh.DvhRessursRestService;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.gql.GraphQLConfig;
import no.nav.nom.apps.gql.AbstractGraphQlController;
import no.nav.nom.apps.gql.GraphqlSupportInterface;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.bean.override.mockito.MockReset;

@Import({
     TestTokensGenerator.class

    , AbstractGraphQlController.class
    , RessursResolver.class
    , RessursPropertyResolver.class
    , OrgtilknytningResolver.class
    , LederResolver.class
    , OrgEnhetResolver.class
    , OrganiseringResolver.class
    , SearchResolver.class
//    , RemedyEnhetIdResolver.class

//    , GraphQLConfig.class
    , GraphQLTestUtil.class

    , ActiveDirectoryRessursBatchLoader.class
    , AdClient.class
    , NomProxyClient.class
    , WebClientAutoConfiguration.class

    , RessursStateRepositoryImpl.class
//    , RessursMetadataRepositoryImpl.class
//    , OrgenhetService.class
    , OrgEnhetStateRepositoryImpl.class
    , OrganiseringStateRepositoryImpl.class
    , OrgtilknytningStateRepositoryImpl.class
    , OrgEnhetLederStateRepositoryImpl.class
})
@EnableConfigurationProperties(value = AppProperties.class)
public class GraphqlTestConfig {
    @Bean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager() {
        return Mockito.mock(OAuth2AuthorizedClientManager.class, MockReset.after());
    }
    @Bean
    OrgEnhetCommandService orgEnhetCommandService() { return Mockito.mock(OrgEnhetCommandService.class, MockReset.after()); }
    @Bean
    KafkaTopicReader kafkaTopicReader() { return Mockito.mock(KafkaTopicReader.class, MockReset.after()); }
    @Bean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter() { return Mockito.mock(GraphQLFieldConsumptionCounter.class, MockReset.after()); }

    @Bean
    DvhRessursRestService dvhRessursRestService() { return Mockito.mock(DvhRessursRestService.class, MockReset.after()); }

    @Bean
    GraphqlSupportInterface graphqlSupportInterface(){
        return GraphqlSupportInterface.dummyImplementation();
    }

    @Bean
    GraphQLConfig graphQLConfig() {
        return new GraphQLConfig();
    }

}
