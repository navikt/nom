package no.nav.nom.apps.api.graphql;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsClientImpl;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.EmbeddeKafkaAdminClient;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhService;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
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
import no.nav.nom.apps.api.ressurs.eventsourcing.metadata.RessursMetadataRepositoryImpl;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventKafkaConfig;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventProducer;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRepository;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({
    DvhRestConsumer.class
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
public class GraphQLExtendedIntegrationTestConfig {
    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockitoBean
    NomStateOrganiseringListener nomStateOrganiseringListener;
    @MockitoBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @MockitoBean
    NomStateDelegertLederListener nomStateDelegertLederListener;

    @MockitoBean
    OrgEnhetEventProducer orgEnhetEventProducer;

    @MockitoBean
    OrgEnhetCommandService orgEnhetCommandService;

    @MockitoBean
    KafkaTopicReader kafkaTopicReader;

    @MockitoBean
    RessursEventSourceRepository ressursEventSourceRepository;

    @MockitoBean
    NomMastered nomMastered;

    @MockitoBean
    ValidationRepository validationRepository;

    @MockitoBean
    OrgEnhetTypeHandler orgEnhetTypeHandler;

    @MockitoBean
    RessursStillingLoader ressursStillingLoader;

    @MockitoBean
    EksterneRessurserLoader eksterneRessurserLoader;

    @MockitoBean
    KommunaleRessurserLoader kommunaleRessurserLoader;

    @MockitoBean
    OrganiseringService organiseringService;

    @MockitoBean
    AuthorizedNavident authorizedNavident;

    @MockitoBean
    RemedyEnhetMapperService remedyEnhetMapperService;

    @MockitoBean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    @MockitoBean
    Unleash unleash;

    @MockitoBean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;

//    @MockitoBean
//    DvhOrdsClient dvHOrdsClient;
}
