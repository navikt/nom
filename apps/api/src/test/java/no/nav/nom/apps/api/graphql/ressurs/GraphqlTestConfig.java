package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.GraphQLTestUtil;
import no.nav.nom.apps.api.graphql.common.config.GraphQLConfig;
import no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.controller.GraphQLRessursController;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.eventsourcing.metadata.RessursMetadataRepositoryImpl;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
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

    , GraphQLRessursController.class
    , RessursResolver.class
    , RessursPropertyResolver.class
    , TelefonResolver.class
    , OrgTilknytningResolver.class
    , LederResolver.class

    , GraphQLConfig.class
    , GraphQLTestUtil.class

    , ActiveDirectoryRessursBatchLoader.class
    , AdClient.class
    , NomProxyClient.class
    , WebClientAutoConfiguration.class

    , DvhRepository.class
    , RessursStateRepositoryImpl.class
    , RessursMetadataRepositoryImpl.class
    , OrgenhetService.class
    , OrgEnhetStateRepositoryImpl.class
    , OrganiseringStateRepositoryImpl.class
    , OrgtilknytningStateRepositoryImpl.class
    , OrgtilknytningService.class
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
    OrgEnhetLederService orgEnhetLederService() { return Mockito.mock(OrgEnhetLederService.class, MockReset.after()); }
}
