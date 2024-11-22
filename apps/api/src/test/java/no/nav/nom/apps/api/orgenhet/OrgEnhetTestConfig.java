package no.nav.nom.apps.api.orgenhet;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.EmbeddeKafkaAdminClient;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhService;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetEventKafkaConfig;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetEventProducer;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSourceRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

@Import({
    DvhOrgEnhetMockUtils.class
    , DvhRestConsumer.class
    , DvhRepository.class
    , DvhService.class
    , DvhOrgenhetsLast.class
    , ValidationService.class
    , NomProxyClient.class
    , NomProxyRestClient.class
    , OrgenhetService.class
    , OrgEnhetCommandService.class
    , OrgEnhetEventProducer.class
    , OrgEnhetEventKafkaConfig.class
    , OrgEnhetIdGenerator.class
    , OrgEnhetTypeHandler.class
    , OrgEnhetOverordnetResolver.class
    , OrgEnhetUnderordnetResolver.class
    , OrgEnhetNomNivaaHandler.class
    , DvhStartupLoadMonitor.class
    , KafkaUtils.class
    , KafkaAutoConfiguration.class
    , EmbeddeKafkaAdminClient.class
    , LocalConfiguration.class
    , LeaderElection.class
    , OTUtenforHierarki.class
    , OrgtilknytningService.class
})
@EnableConfigurationProperties(value = AppProperties.class)
@EnableKafka
@AutoConfigureJson
public class OrgEnhetTestConfig {
    @MockBean
    ValidationRepository validationRepository;

    @MockBean
    RessursStateRepository ressursStateRepository;

    @MockBean
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @MockBean
    NomStateRessursListener nomStateRessursListener;
    @MockBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockBean
    NomStateOrganiseringListener nomStateOrganiseringListener;
    @MockBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @MockBean
    NomStateDelegertLederListener nomStateDelegertLederListener;

    @MockBean
    RessursHendelseForEventsourceService ressursHendelseForEventsourceService;

    @MockBean
    KafkaTopicReader kafkaTopicReader;

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
    OrgEnhetEventSourceRepository orgEnhetEventSourceRepository;

    @MockBean
    NomMastered nomMastered;

    @MockBean
    LuceneService luceneService;

    @MockBean
    Unleash unleash;

    @MockBean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;
}

