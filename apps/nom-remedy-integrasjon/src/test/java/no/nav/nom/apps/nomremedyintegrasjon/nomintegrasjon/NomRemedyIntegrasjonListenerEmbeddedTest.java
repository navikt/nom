package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.MockOAuth2ServerInitializer;
import no.nav.nom.apps.nomremedyintegrasjon.NomRemedyIntegrasjonStarter;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate.ErrorStateRepository;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningStateContract;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(
    classes = NomRemedyIntegrasjonStarter.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "org.nom.intern-ressurs-state",
        "org.nom.intern-ressurs-state-dlt",
        "org.nom.intern-orgtilknytning-state",
        "org.nom.intern-orgtilknytning-state-dlt"
    },
    controlledShutdown = true
)
public class NomRemedyIntegrasjonListenerEmbeddedTest {
    private final String ressursStateTopic = "org.nom.intern-ressurs-state";
    private final String orgtilknytningStateTopic = "org.nom.intern-orgtilknytning-state";

    @MockitoBean
    MasteredRessursStateRepository masteredRessursStateRepository;
    @MockitoBean
    AggregateStateRepository aggregateStateRepository;
    @MockitoBean
    LeaderElection leaderElection;
    @MockitoBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @MockitoBean
    NomStateRessursListener nomStateRessursListener;
    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockitoBean
    ErrorStateRepository errorStateRepository;
    @MockitoBean
    NomRemedyIntegrasjonService nomRemedyIntegrasjonService;
    @MockitoBean
    AdminController adminController;
    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private KafkaTemplate<String, RessursState> ressursStateKafkaTemplate;
    @Autowired
    private KafkaTemplate<String, OrgtilknytningState> orgtilknytningStateKafkaTemplate;
    @MockitoBean
    private Unleash unleash;

    @BeforeEach
    void setUp() {
        when(nomStateRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(nomStateOrgtilknytningListener.hasReadToEndOnStartup()).thenReturn(true);
        when(nomStateMasteredRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(leaderElection.isLeader()).thenReturn(true);
        when(unleash.isEnabled(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Verifiser at ressursState melding lagrer en aggregateState i databasen")
    void test_001() {
        var ressursState = ressursState();
        when(masteredRessursStateRepository.isMastered(ressursState.navident())).thenReturn(true);
        when(aggregateStateRepository.findByStateFid(any())).thenReturn(List.of());

        ressursStateKafkaTemplate.send(ressursStateTopic, ressursState.fid(), ressursState);
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() ->
                verify(aggregateStateRepository, times(1)).save(any(AggregateState.class))
            );
    }

    @Test
    @DisplayName("Verifiser at orgtilknytningState melding lagrer en aggregateState i databasen")
    void test_002() {
        var orgtilknytning = orgtilknytningStateContract();
        when(masteredRessursStateRepository.isMastered(orgtilknytning.navident())).thenReturn(true);
        when(aggregateStateRepository.findAggregateStatesByNavidentAndMasseflyttetFraFidAndStateType(any(), any(), any())).thenReturn(List.of());


        orgtilknytningStateKafkaTemplate.send(orgtilknytningStateTopic, orgtilknytning.fid(), orgtilknytning);
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() ->
                verify(aggregateStateRepository, times(1)).save(any(AggregateState.class))
            );
    }

    @Test
    @DisplayName("Verifiser at meldingen blir sendt til DLT topic ved feil")
    void test_003() {
        // Arrange
        String dlqTopic = ressursStateTopic + "-dlt";
        RessursState invalidMessage = null;

        // Act
        ressursStateKafkaTemplate.send(ressursStateTopic, "key", null);

        // Assert

        Map<String, Object> props = KafkaTestUtils.consumerProps("dlt-test-group", "false", embeddedKafkaBroker);
        JsonDeserializer<RessursState> deserializer = new JsonDeserializer<>(RessursState.class);
        deserializer.addTrustedPackages("*");
        Consumer<String, RessursState> ressursStateConsumer = new KafkaConsumer<>(props, new StringDeserializer(), deserializer);
        ressursStateConsumer.subscribe(Collections.singletonList(dlqTopic));

        ConsumerRecord<String, RessursState> dlqRecord = KafkaTestUtils.getSingleRecord(ressursStateConsumer, dlqTopic);
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(dlqRecord).isNotNull();
                assertThat(dlqRecord.topic()).isEqualTo(dlqTopic);
                assertThat(dlqRecord.key()).isEqualTo("key");
                assertThat(dlqRecord.value()).isEqualTo(invalidMessage);
            });
        ressursStateConsumer.close();
    }

    @Test
    @DisplayName("Verifiser at meldingen blir sendt til DLT topic ved feil")
    void test_004() {
        // Arrange
        String dlqTopic = orgtilknytningStateTopic + "-dlt";
        OrgtilknytningState invalidMessage = null;

        // Act
        orgtilknytningStateKafkaTemplate.send(orgtilknytningStateTopic, "key", null);

        // Assert

        Map<String, Object> props = KafkaTestUtils.consumerProps("dlt-test-group", "false", embeddedKafkaBroker);
        JsonDeserializer<OrgtilknytningState> deserializer = new JsonDeserializer<>(OrgtilknytningState.class);
        deserializer.addTrustedPackages("*");
        Consumer<String, OrgtilknytningState> orgtilknytningStateConsumer = new KafkaConsumer<>(props, new StringDeserializer(), deserializer);
        orgtilknytningStateConsumer.subscribe(Collections.singletonList(dlqTopic));

        ConsumerRecord<String, OrgtilknytningState> dlqRecord = KafkaTestUtils.getSingleRecord(orgtilknytningStateConsumer, dlqTopic);
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(dlqRecord).isNotNull();
                assertThat(dlqRecord.topic()).isEqualTo(dlqTopic);
                assertThat(dlqRecord.key()).isEqualTo("key");
                assertThat(dlqRecord.value()).isEqualTo(invalidMessage);
            });
        orgtilknytningStateConsumer.close();
    }
}
