package no.nav.nom.apps.api.graphql;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.graphql.common.config.ExceptionhandlerConfig;
import no.nav.nom.apps.api.graphql.common.config.GraphQLConfig;
import no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.resolver.FolkeregisterPersonResolver;
import no.nav.nom.apps.api.graphql.controller.GraphQLOrgEnhetController;
import no.nav.nom.apps.api.graphql.controller.GraphQLPersonController;
import no.nav.nom.apps.api.graphql.controller.GraphQLRessursController;
import no.nav.nom.apps.api.graphql.controller.GraphQLSearchController;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetKoblingResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrganiseringResolver;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.person.PersonResolver;
import no.nav.nom.apps.api.graphql.ressurs.OrgTilknytningResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursPropertyResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursResolver;
import no.nav.nom.apps.api.graphql.ressurs.TelefonResolver;
import no.nav.nom.apps.api.graphql.search.SearchResolver;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationRepository;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.app.RessursLedereResolver;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRepository;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Import({
     TestTokensGenerator.class
    , NomProxyClient.class
    , GraphQLOrgEnhetController.class
    , GraphQLPersonController.class
    , GraphQLRessursController.class
    , GraphQLSearchController.class
    , OrgEnhetResolver.class
    , OrgEnhetKoblingResolver.class
    , OrganiseringResolver.class
    , no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver.class
    , no.nav.nom.apps.api.graphql.ressurs.LederResolver.class
    , RemedyEnhetIdResolver.class
    , PersonResolver.class
    , FolkeregisterPersonResolver.class
    , PersonBatchLoader.class
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
})
@EnableConfigurationProperties(value = AppProperties.class)
public class GraphqlTestConfig {
    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
    @MockBean
    OrgEnhetCommandService orgEnhetCommandService;
    @MockBean
    KafkaTopicReader kafkaTopicReader;
    @MockBean
    RemedyEnhetMapperService remedyEnhetMapperService;
    @MockBean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;
    @MockBean
    RessursCommandService ressursCommandService;
    @MockBean
    NomMastered nomMastered;
    @MockBean
    DvhOrgenhetsLast dvhOrgenhetsLast;
    @MockBean
    OrgEnhetTypeHandler orgEnhetTypeHandler;
    @MockBean
    RessursStillingLoader ressursStillingLoader;
    @MockBean
    EksterneRessurserLoader eksterneRessurserLoader;
    @MockBean
    OrganiseringService organiseringService;
    @MockBean
    KommunaleRessurserLoader kommunaleRessurserLoader;
    @MockBean
    AuthorizedNavident authorizedNavident;
    @MockBean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;
    @MockBean
    NomStateDelegertLederListener nomStateDelegertLederListener;
    @MockBean
    RessursEventSourceRepository ressursEventSourceRepository;
    @MockBean
    ValidationRepository validationRepository;
    @MockBean
    Unleash unleash;
}
