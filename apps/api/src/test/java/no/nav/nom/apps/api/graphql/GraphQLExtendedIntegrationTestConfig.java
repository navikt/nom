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
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.DvhOldTdvService;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhService;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.lucenesearch.OrgEnhetStateVisitor;
import no.nav.nom.apps.api.lucenesearch.RessursStateVisitor;
import no.nav.nom.apps.api.organisering.DvhOrganiseringMockUtils;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

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
    , NomProxyRestClient.class
    , PdlUtils.class
    , LocalConfiguration.class
    , DvhOrgEnhetMockUtils.class
    , DvhOrganiseringMockUtils.class
    , KafkaAutoConfiguration.class
    , EmbeddeKafkaAdminClient.class
    , LeaderElection.class
    , OTUtenforHierarki.class
    , OrgtilknytningService.class
    , DvhOldTdvService.class
    , DvhOrdsClientImpl.class
})
@EnableConfigurationProperties(value = AppProperties.class)
@EnableKafka
public class GraphQLExtendedIntegrationTestConfig {
    @MockBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockBean
    NomStateOrganiseringListener nomStateOrganiseringListener;
    @MockBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @MockBean
    NomStateDelegertLederListener nomStateDelegertLederListener;

    @MockBean
    OrgEnhetEventProducer orgEnhetEventProducer;

    @MockBean
    OrgEnhetCommandService orgEnhetCommandService;

    @MockBean
    KafkaTopicReader kafkaTopicReader;

    @MockBean
    RessursEventSourceRepository ressursEventSourceRepository;

    @MockBean
    NomMastered nomMastered;

    @MockBean
    ValidationRepository validationRepository;

    @MockBean
    OrgEnhetTypeHandler orgEnhetTypeHandler;

    @MockBean
    RessursStillingLoader ressursStillingLoader;

    @MockBean
    EksterneRessurserLoader eksterneRessurserLoader;

    @MockBean
    KommunaleRessurserLoader kommunaleRessurserLoader;

    @MockBean
    OrganiseringService organiseringService;

    @MockBean
    AuthorizedNavident authorizedNavident;

    @MockBean
    RemedyEnhetMapperService remedyEnhetMapperService;

    @MockBean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    @MockBean
    Unleash unleash;

    @MockBean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;

//    @MockBean
//    DvhOrdsClient dvHOrdsClient;
}
