package no.nav.nom.apps.api.ressurs.implementations.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhService;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.organisering.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventKafkaConfig;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventProducer;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRepository;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.apps.api.u4diff.U4DiffPaaMastrede;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest(
    classes = {
        DvhRestConsumer.class,
        DvhRepository.class,
        RessursHendelseForEventsourceService.class,
        RessursCommandService.class,
        RessursEventProducer.class,
        RessursEventKafkaConfig.class,
        KafkaProperties.class,
        LocalConfiguration.class,
        ObjectMapper.class,
        TelefonService.class,
    })
@EnableKafka
@AutoConfigureJson
@EmbeddedKafka(partitions = 1,  controlledShutdown = true)
public abstract class DvhTestBase {

    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected DvhRepository dvhRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    protected void configureJackson() {
        objectMapper.findAndRegisterModules()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
    }

    // MOCKS
    @MockitoBean
    DvhService dvhService;
    @MockitoBean
    NomStateRessursListener nomStateRessursListener;
    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockitoBean
    NomStateOrganiseringListener nomStateOrganiseringListener;
    @MockitoBean
    NomStateOrgEnhetListener nomStateOrgEnhetListener;
    @MockitoBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @MockitoBean
    NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener;
    @MockitoBean
    LeaderElection leaderElection;
    @MockitoBean
    DvhOrgenhetsLast dvhOrgenhetsLast;
    @MockitoBean
    OrgEnhetTypeHandler orgEnhetTypeHandler;
    @MockitoBean
    RessursStillingLoader ressursStillingLoader;
    @MockitoBean
    EksterneRessurserLoader eksterneRessurserLoader;
    @MockitoBean
    OrganiseringService organiseringService;
    @MockitoBean
    AuthorizedNavident authorizedNavident;
    @MockitoBean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;
    @MockitoBean
    DvhStartupLoadMonitor dvhStartupLoadMonitor;
    @MockitoBean
    NomStateDelegertLederListener nomStateDelegertLederListener;
    @MockitoBean
    NomMastered nomMastered;
    @MockitoBean
    Unleash unleash;
    @MockitoBean
    DvhOrdsService dvhOrdsService;
    @MockitoBean
    RessursStateRepository ressursStateRepository;
    @MockitoBean
    RessursEventSourceRepository ressursEventSourceRepository;
    @MockitoBean
    U4DiffPaaMastrede u4DiffPaaMastrede;
    @MockitoBean
    OTUtenforHierarki oTUtenforHierarki;
    @MockitoBean
    OrgEnhetLederService orgEnhetLederService;
}
