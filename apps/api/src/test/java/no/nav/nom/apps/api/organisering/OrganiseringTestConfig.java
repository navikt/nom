package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhService;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventProducer;
import no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup.OrganiseringEventSourceRepository;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OldOrganiseringStateRepositoryImpl;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateListener;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OldOrgEnhetStateRepositoryImpl;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationService;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateListener;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.OldRessursStateRepositoryImpl;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

@Import({
    DvhOrganiseringMockUtils.class
    , DvhRestConsumer.class
    , DvhRepository.class
    , DvhService.class
    , DvhOrgenhetsLast.class
    , OldRessursStateRepositoryImpl.class
    , ValidationService.class
    , NomProxyClient.class
    , NomProxyRestClient.class
    , OldOrgEnhetStateRepositoryImpl.class
    , OrgEnhetIdGenerator.class
    , DvhStartupLoadMonitor.class
    , OldOrganiseringStateRepositoryImpl.class
    , OrganiseringService.class
    , OrganiseringEventCommandService.class
    , OrganiseringEventProducer.class
    , KafkaProperties.class
    , LocalConfiguration.class
    , LeaderElection.class
})
@EnableConfigurationProperties(value = AppProperties.class)
@EnableKafka
@AutoConfigureJson
public class OrganiseringTestConfig {
    @MockBean
    OrganiseringEventSourceRepository organiseringEventSourceRepository;

    @MockBean
    RessursStateListener ressursStateListener;

    @MockBean
    NomStateRessursListener nomStateRessursListener;
    @MockBean
    NomStateOrgEnhetListener nomStateOrgEnhetListener;
    @MockBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockBean
    NomStateOrganiseringListener nomStateOrganiseringListener;

    @MockBean
    OrgtilknytningStateListener orgtilknytningStateListener;

    @MockBean
    OrgEnhetStateListener orgEnhetStateListener;

    @MockBean
    RessursHendelseForEventsourceService ressursHendelseForEventsourceService;

    @MockBean
    ValidationRepository validationRepository;

    @MockBean
    OrgEnhetCommandService orgEnhetCommandService;

    @MockBean
    OrgEnhetTypeHandler orgEnhetTypeHandler;

    @MockBean
    RessursStillingLoader ressursStillingLoader;

    @MockBean
    EksterneRessurserLoader eksterneRessurserLoader;

    @MockBean
    KommunaleRessurserLoader kommunaleRessurserLoader;

    @MockBean
    AuthorizedNavident authorizedNavident;
}

