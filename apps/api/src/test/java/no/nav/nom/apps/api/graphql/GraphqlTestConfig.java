package no.nav.nom.apps.api.graphql;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsClientImpl;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.EmbeddeKafkaAdminClient;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhService;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.config.ExceptionhandlerConfig;
import no.nav.nom.apps.api.graphql.common.config.GraphQLConfig;
import no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.controller.GraphQLOrgEnhetController;
import no.nav.nom.apps.api.graphql.controller.GraphQLRessursController;
import no.nav.nom.apps.api.graphql.controller.GraphQLSearchController;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetKoblingResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrganiseringResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.ressurs.OrgTilknytningResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursPropertyResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursResolver;
import no.nav.nom.apps.api.graphql.ressurs.TelefonResolver;
import no.nav.nom.apps.api.graphql.search.SearchResolver;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.lucenesearch.OrgEnhetStateVisitor;
import no.nav.nom.apps.api.lucenesearch.RessursStateVisitor;
import no.nav.nom.apps.api.organisering.DvhOrganiseringMockUtils;
import no.nav.nom.apps.api.organisering.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetEventProducer;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.app.RessursLedereResolver;
import no.nav.nom.apps.api.ressurs.api.v1.dvh.DvhRessursRestService;
import no.nav.nom.apps.api.ressurs.eventsourcing.metadata.RessursMetadataRepositoryImpl;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventKafkaConfig;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventProducer;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRepository;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.api.u4diff.U4DiffPaaMastrede;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.bean.override.mockito.MockReset;

@Import({
     TestTokensGenerator.class
    , NomProxyClient.class
    , GraphQLOrgEnhetController.class
    , GraphQLRessursController.class
    , GraphQLSearchController.class
    , OrgEnhetResolver.class
    , OrgEnhetKoblingResolver.class
    , OrganiseringResolver.class
    , no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver.class
    , no.nav.nom.apps.api.graphql.ressurs.LederResolver.class
    , RemedyEnhetIdResolver.class
    , RessursResolver.class
    , RessursPropertyResolver.class
    , ActiveDirectoryRessursBatchLoader.class
    , AdClient.class
    , TelefonResolver.class
    , RessursLedereResolver.class
    , OrgTilknytningResolver.class
    , SearchResolver.class
    , GraphQLConfig.class
    , ExceptionhandlerConfig.class
    , DvhRestConsumer.class
    , DvhRepository.class
    , DvhService.class
    , DvhOrgenhetsLast.class
    , DvhOrdsService.class
    , RessursCommandService.class
    , RessursEventProducer.class
    , RessursEventKafkaConfig.class
    , RessursStateVisitor.class
    , OrgEnhetStateVisitor.class
    , OrgEnhetIdGenerator.class
    , OrgenhetService.class
    , OrgtilknytningService.class
    , DvhRessursRestService.class
    , RessursHendelseForEventsourceService.class
    , RessursMetadataRepositoryImpl.class
    , LuceneService.class
    , DvhStartupLoadMonitor.class
    , KafkaUtils.class
    , ValidationService.class
    , PdlUtils.class
    , LocalConfiguration.class
    , DvhOrgEnhetMockUtils.class
    , DvhOrganiseringMockUtils.class
    , KafkaAutoConfiguration.class
    , EmbeddeKafkaAdminClient.class
    , LeaderElection.class
    , OTUtenforHierarki.class
    , OrgtilknytningService.class
    , DvhOrdsClientImpl.class
    , WebClientAutoConfiguration.class
})
@EnableConfigurationProperties(value = AppProperties.class)
@EnableKafka
public class GraphqlTestConfig {
    @Bean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager() {
        return Mockito.mock(OAuth2AuthorizedClientManager.class, MockReset.after());
    }
    @Bean
    OrgEnhetCommandService orgEnhetCommandService() {
        return Mockito.mock(OrgEnhetCommandService.class, MockReset.after());
    }
    @Bean
    KafkaTopicReader kafkaTopicReader() {
        return Mockito.mock(KafkaTopicReader.class, MockReset.after());
    }
    @Bean
    RemedyEnhetMapperService remedyEnhetMapperService() {
        return Mockito.mock(RemedyEnhetMapperService.class, MockReset.after());
    }
    @Bean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter() {
        return Mockito.mock(GraphQLFieldConsumptionCounter.class, MockReset.after());
    }
    @Bean
    RessursCommandService ressursCommandService() {
        return Mockito.mock(RessursCommandService.class, MockReset.after());
    }
    @Bean
    NomMastered nomMastered() {
        return Mockito.mock(NomMastered.class, MockReset.after());
    }
    @Bean
    DvhOrgenhetsLast dvhOrgenhetsLast() {
        return Mockito.mock(DvhOrgenhetsLast.class, MockReset.after());
    }
    @Bean
    OrgEnhetTypeHandler orgEnhetTypeHandler() {
        return Mockito.mock(OrgEnhetTypeHandler.class, MockReset.after());
    }
    @Bean
    OrgEnhetEventProducer orgEnhetEventProducer() {
        return Mockito.mock(OrgEnhetEventProducer.class, MockReset.after());
    }
    @Bean
    RessursStillingLoader ressursStillingLoader() {
        return Mockito.mock(RessursStillingLoader.class, MockReset.after());
    }
    @Bean
    EksterneRessurserLoader eksterneRessurserLoader() {
        return Mockito.mock(EksterneRessurserLoader.class, MockReset.after());
    }
    @Bean
    OrganiseringService organiseringService() {
        return Mockito.mock(OrganiseringService.class, MockReset.after());
    }
    @Bean
    KommunaleRessurserLoader kommunaleRessurserLoader() {
        return Mockito.mock(KommunaleRessurserLoader.class, MockReset.after());
    }
    @Bean
    AuthorizedNavident authorizedNavident() {
        return Mockito.mock(AuthorizedNavident.class, MockReset.after());
    }
    @Bean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState() {
        return Mockito.mock(ManuelleRessurserOgLedereTilState.class, MockReset.after());
    }
    @Bean
    RessursEventSourceRepository ressursEventSourceRepository() {
        return Mockito.mock(RessursEventSourceRepository.class, MockReset.after());
    }
    @Bean
    ValidationRepository validationRepository() {
        return Mockito.mock(ValidationRepository.class, MockReset.after());
    }
    @Bean
    Unleash unleash() {
        return Mockito.mock(Unleash.class, MockReset.after());
    }
    @Bean
    U4DiffPaaMastrede u4DiffPaaMastrede() {
        return Mockito.mock(U4DiffPaaMastrede.class, MockReset.after());
    }
}
