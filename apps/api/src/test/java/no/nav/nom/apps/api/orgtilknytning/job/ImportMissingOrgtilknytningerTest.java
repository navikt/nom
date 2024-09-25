package no.nav.nom.apps.api.orgtilknytning.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningTilknytningtypeEndretEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.nom.apps.api.orgtilknytning.job.ImportMissingOrgtilknytninger.MissingOrgtilknytning;
import static no.nav.nom.contract.nom.Tilknytningtype.INGEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
class ImportMissingOrgtilknytningerTest extends TestBase {

    public final static String NAVIDENT = "A123456";
    public final static String NAVN = "Kari Nordmann";
    public final static String ORG_ID = "ab123c";
    public final static String ORG_NAVN = "NAV-kontor 1";
    public final static LocalDate DATE_IN_2024 = LocalDate.of(2024, 8, 1);
    public final static LocalDate DATE_IN_2023 = LocalDate.of(2023, 1, 1);
    public final static LocalDate DATE_IN_2019 = LocalDate.of(2019, 4, 4);

    private static final String ORGTILKNYTNING_TOPIC = "org.nom.orgtilknytning-eventsource";
    private static final String NOM_MASTERED_TOPIC = "org.nom.nom-mastered-ressurs";


    @Autowired
    private ImportMissingOrgtilknytninger importMissingOrgtilknytninger;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MasteredRessursStateRepository masteredRessursStateRepository;


    @MockBean
    private RessursStateRepository ressursStateRepositoryMock;

    @MockBean
    private OrgEnhetStateRepository orgEnhetStateRepositoryMock;

    @MockBean
    private OrgtilknytningStateRepository orgtilknytningStateRepositoryMock;


    BlockingQueue<ConsumerRecord<String, OrgtilknytningEvent>> recordsQueue;
    KafkaMessageListenerContainer<String, OrgtilknytningEvent> container;

    BlockingQueue<ConsumerRecord<String, LocalDateTime>> nomMasteredRecordsQueue;
    KafkaMessageListenerContainer<String, LocalDateTime> nomMasteredContainer;


    @BeforeEach
    public void setUp() {
        // Features skal alltid være på i tester
        when(unleash.isEnabled(any())).thenReturn(true);

        // Må nullstilles slik at tester ikke påvirker hverandre
        masteredRessursStateRepository.clear();

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils
                .consumerProps("consumerGroup", "false", embeddedKafkaBroker));

        DefaultKafkaConsumerFactory<String, OrgtilknytningEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(OrgtilknytningEvent.class, objectMapper)
        );

        recordsQueue = new LinkedBlockingQueue<>();

        ContainerProperties containerProperties = new ContainerProperties(ORGTILKNYTNING_TOPIC);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, OrgtilknytningEvent>) recordsQueue::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());


        // Ekstra listener for NOM mastered topic
        nomMasteredRecordsQueue = new LinkedBlockingQueue<>();

        var jsonSerializer = new JsonDeserializer<LocalDateTime>(objectMapper);
        jsonSerializer.addTrustedPackages("java.time");

        DefaultKafkaConsumerFactory<String, LocalDateTime> nomMasteredConsumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                jsonSerializer
        );

        ContainerProperties nomMasteredContainerProperties = new ContainerProperties(NOM_MASTERED_TOPIC);
        nomMasteredContainer = new KafkaMessageListenerContainer<>(nomMasteredConsumerFactory, nomMasteredContainerProperties);
        nomMasteredContainer.setupMessageListener((MessageListener<String, LocalDateTime>) nomMasteredRecordsQueue::add);
        nomMasteredContainer.start();

        ContainerTestUtils.waitForAssignment(nomMasteredContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    public void tearDown() {
        container.stop();
        nomMasteredContainer.stop();
    }

    @Test
    void should_parse_file_with_header() throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(
                """
                        NAV-ident,Navn,Sektor,Enhetsnummer,Orgenhet Unit4 nr,Orgenhet Unit4 navn,NOM_orgenhet,NOM_orgnavn,gyldig_tom,Kommentar
                        A123456,Kari Nordmann,NAV_KOMMUNAL,1111,1234567,Orgenhet 1,ab123c,Orgenhet 1,31-Dec-99,
                        """
        ));

        List<MissingOrgtilknytning> result = importMissingOrgtilknytninger.readMissingOrgtilknytninger(br);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getFirst().navident()).isEqualTo(NAVIDENT);
    }


    @Test
    void should_generate_messages_for_missing_ot() {
        // Ressurs og OrgEnhet eksisterer
        when(ressursStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(Optional.ofNullable(ressursState()));
        when(orgEnhetStateRepositoryMock.findById(ORG_ID)).thenReturn(Optional.of(orgEnhetState()));

        // Ressursen har ingen orgtilknytninger 
        when(orgtilknytningStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(Collections.emptyList());


        var missingOt = new MissingOrgtilknytning(NAVIDENT, NAVN, ORG_ID, ORG_NAVN);

        importMissingOrgtilknytninger.createEventsForNewOrgtilknytning(missingOt);


        await()
                .atMost(1, SECONDS)
                .pollInterval(10, MILLISECONDS)
                .until(() -> recordsQueue.size() == 3);

        ConsumerRecord<String, OrgtilknytningEvent> opprettOrgtilknytningRecord = recordsQueue.remove();
        assertThat(opprettOrgtilknytningRecord.value()).isInstanceOf(OrgtilknytningOpprettetEventV1.class);
        var event = (OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData) opprettOrgtilknytningRecord.value().getData();
        assertThat(event.navident()).isEqualTo(NAVIDENT);
        assertThat(event.orgEnhetId()).isEqualTo(ORG_ID);

        ConsumerRecord<String, OrgtilknytningEvent> dagligOppfolgingRecord = recordsQueue.remove();
        assertThat(dagligOppfolgingRecord.value()).isInstanceOf(OrgtilknytningDagligOppfolgingEndretEventV1.class);
        var dolEvent = (OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData) dagligOppfolgingRecord.value().getData();
        assertThat(dolEvent.dagligOppfolging()).isTrue();

        ConsumerRecord<String, OrgtilknytningEvent> tilknytningstypeRecord = recordsQueue.remove();
        assertThat(tilknytningstypeRecord.value()).isInstanceOf(OrgtilknytningTilknytningtypeEndretEventV1.class);
        var tilknytningEvent = (OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData) tilknytningstypeRecord.value().getData();
        assertThat(tilknytningEvent.orgtilknytningType()).isEqualTo(Optional.of(INGEN));
    }

    @Test
    void should_not_generate_message_for_existing_and_active_ot() {
        // Ressurs og OrgEnhet eksisterer
        when(ressursStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(Optional.ofNullable(ressursState()));
        when(orgEnhetStateRepositoryMock.findById(ORG_ID)).thenReturn(Optional.of(orgEnhetState()));

        var missingOt = new MissingOrgtilknytning(NAVIDENT, NAVN, ORG_ID, ORG_NAVN);

        var existingOrgtilknytning = OrgtilknytningState.builder()
                .navident(NAVIDENT)
                .orgEnhetId(ORG_ID)
                .gyldigTom(Optional.of(LocalDate.now().plusMonths(2)))
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(List.of(existingOrgtilknytning));


        importMissingOrgtilknytninger.createEventsForNewOrgtilknytning(missingOt);

        // Vente til evt. meldinger har blitt produsert
        await().pollDelay(1, SECONDS).untilAsserted(() -> assertThat(true).isTrue());

        assertThat(recordsQueue.size()).isEqualTo(0);
    }

    @Test
    void should_generate_message_for_existing_ot_that_has_expired() {
        // Ressurs og OrgEnhet eksisterer
        when(ressursStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(Optional.ofNullable(ressursState()));
        when(orgEnhetStateRepositoryMock.findById(ORG_ID)).thenReturn(Optional.of(orgEnhetState()));

        var missingOt = new MissingOrgtilknytning(NAVIDENT, NAVN, ORG_ID, ORG_NAVN);

        var expiredOrgtilknytning = OrgtilknytningState.builder()
                .navident(NAVIDENT)
                .orgEnhetId(ORG_ID)
                .gyldigTom(Optional.of(LocalDate.now().minusDays(1)))
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(List.of(expiredOrgtilknytning));

        
        importMissingOrgtilknytninger.createEventsForNewOrgtilknytning(missingOt);

        
        // Tre meldinger skal sendes
        await()
                .atMost(1, SECONDS)
                .pollInterval(10, MILLISECONDS)
                .until(() -> recordsQueue.size() == 3);
    }

    @Test
    void should_generate_nom_mastered_message_for_missing_ot() {
        // Ressurs og OrgEnhet eksisterer
        when(ressursStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(Optional.ofNullable(ressursState()));
        when(orgEnhetStateRepositoryMock.findById(ORG_ID)).thenReturn(Optional.of(orgEnhetState()));

        // Ressursen har ingen orgtilknytninger
        when(orgtilknytningStateRepositoryMock.findByNavident(NAVIDENT)).thenReturn(Collections.emptyList());

        
        var missingOt = new MissingOrgtilknytning(NAVIDENT, NAVN, ORG_ID, ORG_NAVN);

        importMissingOrgtilknytninger.createEventsForNewOrgtilknytning(missingOt);


        await()
                .atMost(1, SECONDS)
                .pollInterval(10, MILLISECONDS)
                .until(() -> nomMasteredRecordsQueue.size() == 1);

        ConsumerRecord<String, LocalDateTime> nomMasteredRecord = nomMasteredRecordsQueue.remove();
        assertThat(nomMasteredRecord.key()).isEqualTo(NAVIDENT);
    }

    @ParameterizedTest(name = "Orgenhet gyldigFom: {0} Ressurs startdato: {1} OT fomDato: {2}")
    @MethodSource("correctOrgtilknytningFomDatoInput")
    @DisplayName("FomDato for Orgtilknytning skal settes til seneste av orgEnhet.gyldigFom og ressurs.startDato")
    void should_use_latest_date_for_ot_fromDato(
            LocalDate orgEnhetGyldigFom,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<LocalDate> ressursStartDato,
            LocalDate otFromDato
    ) {
        assertThat(importMissingOrgtilknytninger.getFomDato(orgEnhetGyldigFom, ressursStartDato)).isEqualTo(otFromDato);
    }

    private static Stream<Arguments> correctOrgtilknytningFomDatoInput() {
        return Stream.of(
                // Orgenhet gyldigFom, ressurs startdato, forventet resultat
                Arguments.of(DATE_IN_2023, Optional.of(DATE_IN_2024), DATE_IN_2024),
                Arguments.of(DATE_IN_2024, Optional.of(DATE_IN_2019), DATE_IN_2024),
                Arguments.of(DATE_IN_2024, Optional.empty(), DATE_IN_2024)
        );
    }

    public static RessursState ressursState() {
        return RessursState.builder()
                .fid(UUID.randomUUID().toString())
                .navident(NAVIDENT)
                .sektor(Sektor.NAV_KOMMUNAL)
                .startdato(Optional.of(LocalDate.of(2024, 1, 1)))
                .sluttdato(Optional.empty())
                .build();
    }

    public static OrgEnhetState orgEnhetState() {
        return OrgEnhetState.builder()
                .fid(UUID.randomUUID().toString())
                .id(ORG_ID)
                .gyldigFom(LocalDate.now())
                .build();
    }

}