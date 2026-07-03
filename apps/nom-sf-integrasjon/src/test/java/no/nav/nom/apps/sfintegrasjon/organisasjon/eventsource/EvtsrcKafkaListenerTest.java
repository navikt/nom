package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import tools.jackson.databind.json.JsonMapper;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelse;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvtsrcKafkaListenerTest {

    @Mock OrganisasjonHendelseRepository repository;
    @Mock AdminClient evtsrcAdminClient;
    @Mock OrgEnhetStateRepository orgEnhetStateRepository;
    @Mock OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    @Mock OrganiseringStateRepository organiseringStateRepository;
    @Mock KafkaListenerEndpointRegistry listenerRegistry;
    @Mock ConsumerSeekAware.ConsumerSeekCallback callback;

    private EvtsrcKafkaListener listener;
    private EvtsrcKafkaProperties properties;

    private static final String ORGENHET_TOPIC       = "org.nom.orgenhet-eventsource";
    private static final String ORGENHET_LEDER_TOPIC = "org.nom.orgenhet-leder-eventsource";
    private static final String ORGANISERING_TOPIC   = "org.nom.organisering-eventsource";

    @BeforeEach
    void setUp() {
        properties = new EvtsrcKafkaProperties();
        properties.setGroupId("nom-sf-integrasjon-evtsrc-test");
        properties.setRetryDelayMs(10);
        properties.getOrgenhet().setTopic(ORGENHET_TOPIC);
        properties.getOrgenhetLeder().setTopic(ORGENHET_LEDER_TOPIC);
        properties.getOrganisering().setTopic(ORGANISERING_TOPIC);

        var objectMapper = JsonMapper.builder().build();
        listener = new EvtsrcKafkaListener(
            repository, objectMapper, properties, evtsrcAdminClient,
            orgEnhetStateRepository, orgEnhetLederStateRepository, organiseringStateRepository, listenerRegistry
        );
    }

    // ── orgenhetListener ──────────────────────────────────────────────────────

    @Test
    @DisplayName("orgenhetListener: orgEnhetId hentes fra NOM state (state.id)")
    void orgenhetListener_orgEnhetIdFraNomState() {
        stubOrgEnhetState("orgenhet-fid-42", "NAV123456");
        var melding = """
            {"type":"OrgEnhetOpprettetEventV1","fid":"orgenhet-fid-42"}
            """.strip();

        listener.orgenhetListener(melding, "kafka-key-42");

        var captor = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
        verify(repository).save(captor.capture());
        var lagret = captor.getValue();
        assertThat(lagret.getFid()).isEqualTo("orgenhet-fid-42");
        assertThat(lagret.getOrgEnhetId()).isEqualTo("NAV123456");
        assertThat(lagret.getEventType()).isEqualTo("OrgEnhetOpprettetEventV1");
        assertThat(lagret.getEventKey()).isEqualTo("kafka-key-42");
        assertThat(lagret.getMottattTidspunkt()).isNotNull();
        assertThat(lagret.getBehandlingsId()).isNull();
    }

    @Test
    @DisplayName("orgenhetListener: state ikke funnet ved første forsøk, funnet etter 2 sek retry")
    void orgenhetListener_stateFunnetEtterRetry() {
        var state = OrgEnhetState.builder().fid("fid-1").id("NAV001").build();
        when(orgEnhetStateRepository.findByFid("fid-1"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(state));

        listener.orgenhetListener(
            """
            {"type":"OrgEnhetEndretNavnEventV1","fid":"fid-1"}
            """.strip(), "key-1");

        var captor = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrgEnhetId()).isEqualTo("NAV001");
    }

    @Test
    @DisplayName("orgenhetListener: kaster OrgEnhetStateIkkeFunnetException etter to mislykkede forsøk")
    void orgenhetListener_kastertNaarStateIkkeFunnetEtterRetry() {
        when(orgEnhetStateRepository.findByFid("ukjent-fid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.orgenhetListener(
            """
            {"type":"OrgEnhetOpprettetEventV1","fid":"ukjent-fid"}
            """.strip(), "key-x"))
            .isInstanceOf(OrgEnhetStateIkkeFunnetException.class);

        verify(repository, never()).save(any());
    }

    // ── orgenhetLederListener ─────────────────────────────────────────────────

    @Test
    @DisplayName("orgenhetLederListener: orgEnhetId hentes fra NOM orgenhet-leder state (state.orgEnhetId)")
    void orgenhetLederListener_orgEnhetIdFraNomState() {
        var state = OrgEnhetLederState.builder().fid("leder-fid-1").orgEnhetId("NAV-ORG-1").build();
        when(orgEnhetLederStateRepository.findByFid("leder-fid-1")).thenReturn(Optional.of(state));

        listener.orgenhetLederListener(
            """
            {"type":"OrgEnhetLederOpprettetEventV1","fid":"leder-fid-1","data":{"navident":"A123456"}}
            """.strip(), "leder-key-1");

        var captor = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrgEnhetId()).isEqualTo("NAV-ORG-1");
    }

    @Test
    @DisplayName("orgenhetLederListener: kaster OrgEnhetStateIkkeFunnetException etter to mislykkede forsøk")
    void orgenhetLederListener_kastertNaarStateIkkeFunnetEtterRetry() {
        when(orgEnhetLederStateRepository.findByFid("ukjent-fid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.orgenhetLederListener(
            """
            {"type":"OrgEnhetLederSlettetEventV1","fid":"ukjent-fid","data":{}}
            """.strip(), "leder-key-x"))
            .isInstanceOf(OrgEnhetStateIkkeFunnetException.class);

        verify(repository, never()).save(any());
    }

    // ── organiseringListener ──────────────────────────────────────────────────

    @Test
    @DisplayName("organiseringListener: orgEnhetId hentes fra NOM organisering state (state.id)")
    void organiseringListener_orgEnhetIdFraNomState() {
        var state = OrganiseringState.builder().fid("org-fid-7").id("NAV-ORG-7").build();
        when(organiseringStateRepository.findByFid("org-fid-7")).thenReturn(Optional.of(state));

        listener.organiseringListener(
            """
            {"type":"OrganiseringOpprettetEventV1","fid":"org-fid-7"}
            """.strip(), "org-key-7");

        var captor = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFid()).isEqualTo("org-fid-7");
        assertThat(captor.getValue().getOrgEnhetId()).isEqualTo("NAV-ORG-7");
    }

    @Test
    @DisplayName("organiseringListener: kaster OrgEnhetStateIkkeFunnetException etter to mislykkede forsøk")
    void organiseringListener_kastertNaarStateIkkeFunnetEtterRetry() {
        when(organiseringStateRepository.findByFid("ukjent-fid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.organiseringListener(
            """
            {"type":"OrganiseringOpprettetEventV1","fid":"ukjent-fid"}
            """.strip(), "key-y"))
            .isInstanceOf(OrgEnhetStateIkkeFunnetException.class);

        verify(repository, never()).save(any());
    }

    // ── validering ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Kaster IllegalArgumentException når fid mangler i melding")
    void kastertUnnakNaarFidMangler() {
        var melding = """
            {"type":"OrgEnhetOpprettetEventV1"}
            """.strip();

        assertThatThrownBy(() -> listener.orgenhetListener(melding, "key-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fid");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Kaster IllegalArgumentException når type mangler i melding")
    void kastertUnnakNaarTypeMangler() {
        stubOrgEnhetState("orgenhet-fid-99", "NAV099");
        var melding = """
            {"fid":"orgenhet-fid-99"}
            """.strip();

        assertThatThrownBy(() -> listener.orgenhetListener(melding, "key-2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("type");

        verify(repository, never()).save(any());
    }

    // ── onPartitionsAssigned ──────────────────────────────────────────────────

    @Test
    @DisplayName("onPartitionsAssigned: seek til startOffset når ingen committed offsets finnes")
    void onPartitionsAssigned_seekNaarIngenCommittedOffsets() throws Exception {
        properties.getOrgenhet().setStartOffset(500L);
        var tp = new TopicPartition(ORGENHET_TOPIC, 0);
        stubAdminClient(Map.of());

        listener.onPartitionsAssigned(Map.of(tp, 0L), callback);

        verify(callback).seek(ORGENHET_TOPIC, 0, 500L);
    }

    @Test
    @DisplayName("onPartitionsAssigned: ingen seek når startOffset er -1 (latest)")
    void onPartitionsAssigned_ingenSeekNaarStartOffsetErMinusEn() throws Exception {
        properties.getOrgenhet().setStartOffset(-1L);
        var tp = new TopicPartition(ORGENHET_TOPIC, 0);
        stubAdminClient(Map.of());

        listener.onPartitionsAssigned(Map.of(tp, 0L), callback);

        verify(callback, never()).seek(any(), any(Integer.class), any(Long.class));
    }

    @Test
    @DisplayName("onPartitionsAssigned: ingen seek når committed offset allerede finnes")
    void onPartitionsAssigned_ingenSeekNaarCommittedOffsetFinnes() throws Exception {
        properties.getOrgenhet().setStartOffset(500L);
        var tp = new TopicPartition(ORGENHET_TOPIC, 0);
        stubAdminClient(Map.of(tp, new OffsetAndMetadata(123L)));

        listener.onPartitionsAssigned(Map.of(tp, 123L), callback);

        verify(callback, never()).seek(any(), any(Integer.class), any(Long.class));
    }

    // ── Faktiske eventsource-meldinger ────────────────────────────────────────────

    private static final JsonMapper EVTSRC_MAPPER = JsonMapper.builder().build();

    @ParameterizedTest(name = "{0}")
    @MethodSource("orgenhetMeldinger")
    @DisplayName("orgenhetListener: faktisk melding fra orgenhet-eventsource topic")
    void orgenhetListener_faktiskMelding(String filnavn, String melding, String fid, String expectedEventType) {
        stubOrgEnhetState(fid, "854600");

        listener.orgenhetListener(melding, "key-" + filnavn);

        var captor = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
        verify(repository).save(captor.capture());
        var lagret = captor.getValue();
        assertThat(lagret.getFid()).isEqualTo(fid);
        assertThat(lagret.getEventType()).isEqualTo(expectedEventType);
        assertThat(lagret.getOrgEnhetId()).isEqualTo("854600");
        assertThat(lagret.getEventKey()).isEqualTo("key-" + filnavn);
        assertThat(lagret.getMelding()).isEqualTo(melding);
        assertThat(lagret.getMottattTidspunkt()).isNotNull();
    }

    static Stream<Arguments> orgenhetMeldinger() {
        return Stream.of(
            lesMeldingArgs("evtsrcmeldinger/orgenhet/NomNivaaEndretEventV1_1.json"),
            lesMeldingArgs("evtsrcmeldinger/orgenhet/OrgEnhetAvvikletEventV1_1.json"),
            lesMeldingArgs("evtsrcmeldinger/orgenhet/OrgEnhetKorrigertEventV1_1.json"),
            lesMeldingArgs("evtsrcmeldinger/orgenhet/OrgEnhetOpprettetEventV1_1.json"),
            lesMeldingArgs("evtsrcmeldinger/orgenhet/OrgEnhetOpprettetEventV1_2.json")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("orgenhetLederMeldinger")
    @DisplayName("orgenhetLederListener: faktisk melding fra orgenhet-leder-eventsource topic")
    void orgenhetLederListener_faktiskMelding(String filnavn, String melding, String fid, String expectedEventType) {
        // SluttdatoOppdatert og StartdatoOppdatert har ingen orgEnhetId i data —
        // bevis på at state-oppslag er nødvendig for alle event-typer.
        var state = OrgEnhetLederState.builder().fid(fid).orgEnhetId("ka609r").build();
        when(orgEnhetLederStateRepository.findByFid(fid)).thenReturn(Optional.of(state));

        listener.orgenhetLederListener(melding, "key-" + filnavn);

        var captor = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
        verify(repository).save(captor.capture());
        var lagret = captor.getValue();
        assertThat(lagret.getFid()).isEqualTo(fid);
        assertThat(lagret.getEventType()).isEqualTo(expectedEventType);
        assertThat(lagret.getOrgEnhetId()).isEqualTo("ka609r");
        assertThat(lagret.getMelding()).isEqualTo(melding);
        assertThat(lagret.getMottattTidspunkt()).isNotNull();
    }

    static Stream<Arguments> orgenhetLederMeldinger() {
        return Stream.of(
            lesMeldingArgs("evtsrcmeldinger/orgenhetleder/OrgEnhetLederOpprettetEventV1_1.json"),
            lesMeldingArgs("evtsrcmeldinger/orgenhetleder/OrgEnhetLederSluttdatoOppdatertEventV1_1.json"),
            lesMeldingArgs("evtsrcmeldinger/orgenhetleder/OrgEnhetLederStartdatoOppdatertEventV1_1.json")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("organiseringMeldinger")
    @DisplayName("organiseringListener: faktisk melding fra organisering-eventsource topic")
    void organiseringListener_faktiskMelding(String filnavn, String melding, String fid, String expectedEventType) {
        var state = OrganiseringState.builder().fid(fid).id("fy872h").build();
        when(organiseringStateRepository.findByFid(fid)).thenReturn(Optional.of(state));

        listener.organiseringListener(melding, "key-" + filnavn);

        var captor = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
        verify(repository).save(captor.capture());
        var lagret = captor.getValue();
        assertThat(lagret.getFid()).isEqualTo(fid);
        assertThat(lagret.getEventType()).isEqualTo(expectedEventType);
        assertThat(lagret.getOrgEnhetId()).isEqualTo("fy872h");
        assertThat(lagret.getMelding()).isEqualTo(melding);
        assertThat(lagret.getMottattTidspunkt()).isNotNull();
    }

    static Stream<Arguments> organiseringMeldinger() {
        return Stream.of(
            lesMeldingArgs("evtsrcmeldinger/organisering/OrganiseringAvsluttetEventV1_1.json"),
            lesMeldingArgs("evtsrcmeldinger/organisering/OrganiseringOpprettetEventV1_1.json")
        );
    }

    // ── hjelpemetoder ─────────────────────────────────────────────────────────

    private static Arguments lesMeldingArgs(String sti) {
        String melding = lesMelding(sti);
        String filnavn = sti.substring(sti.lastIndexOf('/') + 1);
        try {
            var root = EVTSRC_MAPPER.readTree(melding);
            return Arguments.of(filnavn, melding, root.path("fid").asText(), root.path("type").asText());
        } catch (Exception e) {
            throw new RuntimeException("Feil ved parsing av " + sti, e);
        }
    }

    private static String lesMelding(String sti) {
        try (var is = EvtsrcKafkaListenerTest.class.getClassLoader().getResourceAsStream(sti)) {
            return new String(java.util.Objects.requireNonNull(is, "Fant ikke: " + sti).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Fant ikke testfil: " + sti, e);
        }
    }

    private void stubOrgEnhetState(String fid, String id) {
        when(orgEnhetStateRepository.findByFid(fid))
            .thenReturn(Optional.of(OrgEnhetState.builder().fid(fid).id(id).build()));
    }

    @SuppressWarnings("unchecked")
    private void stubAdminClient(Map<TopicPartition, OffsetAndMetadata> committedOffsets) throws Exception {
        var mockResult = mock(ListConsumerGroupOffsetsResult.class);
        var mockFuture = (KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>) mock(KafkaFuture.class);
        when(evtsrcAdminClient.listConsumerGroupOffsets("nom-sf-integrasjon-evtsrc-test")).thenReturn(mockResult);
        when(mockResult.partitionsToOffsetAndMetadata()).thenReturn(mockFuture);
        when(mockFuture.get(10, TimeUnit.SECONDS)).thenReturn(committedOffsets);
    }
}
