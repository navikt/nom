package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class OrganiseringTest extends OrganiseringTestBase {

    private static final String TOPIC = "org.nom.organisering-eventsource";

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @MockitoBean
    private OrgenhetService orgenhetService;

    @MockitoBean
    Unleash unleash;

    @MockitoBean
    DvhOrdsService dvhOrdsService;

    @Autowired
    private OrganiseringStateRepository organiseringStateRepository;

    BlockingQueue<ConsumerRecord<String, OrganiseringEvent>> recordsQueue;

    KafkaMessageListenerContainer<String, OrganiseringEvent> container;


    @BeforeEach
    public void setUp() throws JsonProcessingException {
        var start = System.currentTimeMillis();
        // Always enable features for tests
        when(unleash.isEnabled(any())).thenReturn(true);

        organiseringStateRepository.clear();

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils
            .consumerProps("consumerGroup", "false", embeddedKafkaBroker));

        DefaultKafkaConsumerFactory<String, OrganiseringEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(
            consumerProps,
            new StringDeserializer(),
            new JsonDeserializer<>(OrganiseringEvent.class, objectMapper)
        );

        recordsQueue = new LinkedBlockingQueue<>();

        ContainerProperties containerProperties = new ContainerProperties(TOPIC);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, OrganiseringEvent>) recordsQueue::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        log.info("Setting up Kafka consumer took {} ms", System.currentTimeMillis() - start);
    }

    @AfterEach
    public void tearDown() {
        container.stop();
    }


    @Test
    @DisplayName("Opprett en organisering")
    void opprett_en_organisering() throws  InterruptedException {

        // Mock DVH API response
        var dvhOrganisering = DvhOrganisering.builder()
                .agressoIdUnder("1")
                .navnUnder("Navn_1")
                .orgNivUnder("Niva_1")
                .agressoIdOver("2")
                .navnOver("Navn_2")
                .orgNivOver("Niva_2")
                .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(dvhOrganisering));

        // Mock OrgenhetService where we find NOM id
        var orgeEnhetState = mockOrgenhetServiceGetEnhet("mock_id", dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder()
        );

        var orgeEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver()
        );

        // Get organiseringer from DVH
        dvhRestConsumer.loadOrganiseringerFromDvh();
        

        ConsumerRecord<String, OrganiseringEvent> singleRecord = recordsQueue.poll(250, MILLISECONDS);

        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        var eventData = (OrganiseringOpprettetEventData) singleRecord.value().getData();

        assertThat(eventData.idOver()).isEqualTo(orgeEnhetStateOver.id());
        assertThat(eventData.navnOver()).isEqualTo(dvhOrganisering.getNavnOver());
        assertThat(eventData.id()).isEqualTo(orgeEnhetState.id());
        assertThat(eventData.navn()).isEqualTo(dvhOrganisering.getNavnUnder());
        assertThat(eventData.gyldigFom()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("ignorere processering av organisering når tom liste mottas fra DVH")
    void ignorerer_processering_organisering_nar_tom_lista_mottas_fra_dvh() throws JsonProcessingException {

        // Receive organisering from DVH
        var dvhOrganisering = DvhOrganisering.builder()
            .agressoIdUnder("1")
            .navnUnder("Navn_1")
            .orgNivUnder("Niva_1")
            .agressoIdOver("2")
            .navnOver("Navn_2")
            .orgNivOver("Niva_2")
            .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(dvhOrganisering));

        // Mock OrgenhetService where we find NOM id
        var orgEnhetState = mockOrgenhetServiceGetEnhet("mock_id", dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder()
        );

        var orgEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver()
        );

        // First import from DVH, creates the organisering
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // The organisering is now present in the state
        OrganiseringState organiseringState = getOrganiseringState("organisering_1", orgEnhetState, orgEnhetStateOver, LocalDate.now(), null);
        organiseringStateRepository.update(organiseringState);

        // No longer receive organisering from DVH
        when(dvhOrdsService.getOrganiseringer()).thenReturn(emptyList());

        // Second import from DVH, this should not create avslutt organisering event
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // Expect one messages
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 1);

        var createRecord = recordsQueue.remove();
        assertThat(createRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
    }

    @Test
    @DisplayName("Avslutt en organisering og opprett på nytt")
    void avslutt_og_opprett_en_organisering() {

        // Receive organisering from DVH
        var dvhOrganisering = DvhOrganisering.builder()
            .agressoIdUnder("1")
            .navnUnder("Navn_1")
            .orgNivUnder("Niva_1")
            .agressoIdOver("2")
            .navnOver("Navn_2")
            .orgNivOver("Niva_2")
            .build();

        var dummyDvhOrganisering = DvhOrganisering.builder()   // Enheter ikke tilgjengelig i Orgenhet state - vil da ikke bli prosessert
            .agressoIdUnder("1x")
            .navnUnder("Navn_1x")
            .orgNivUnder("Niva_1x")
            .agressoIdOver("2x")
            .navnOver("Navn_2x")
            .orgNivOver("Niva_2x")
            .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(dvhOrganisering));

        // Mock OrgenhetService where we find NOM id
        var orgEnhetState = mockOrgenhetServiceGetEnhet("mock_id", dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder()
        );

        var orgEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver()
        );

        // First import from DVH, creates the organisering
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // The organisering is now present in the state
        OrganiseringState organiseringState = getOrganiseringState("organisering_1", orgEnhetState, orgEnhetStateOver, LocalDate.now(), null);
        organiseringStateRepository.update(organiseringState);

        // 2. No longer receive organisering from DVH
        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(dummyDvhOrganisering));

        // Second import from DVH, this should not create avslutt organisering event
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // 3. The gyldigTom date of organisering present in the state is NOT anymore in the future
        var newGyldigTom = LocalDate.now().minusDays(5);
        var organiseringState_avsluttet = organiseringState.toBuilder()
            .gyldigTom(newGyldigTom)
            .build();
        organiseringStateRepository.putEventAggregate(organiseringState_avsluttet);

        // Change organisering from DVH. Organisering is same as the one received in step 1, but it will have new fid
        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(dvhOrganisering));

        // Third import from DVH, creates avslutt organisering event
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // Expect three messages
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 3);

        var createRecord = recordsQueue.remove();
        var terminateRecord = recordsQueue.remove();
        var newCreateRecord = recordsQueue.remove();
        assertThat(createRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        assertThat(terminateRecord.value().getData()).isExactlyInstanceOf(OrganiseringAvsluttetEventData.class);
        assertThat(newCreateRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);

        var terminateMessage = (OrganiseringAvsluttetEventData) terminateRecord.value().getData();
        assertThat(terminateMessage.gyldigTom()).isEqualTo(LocalDate.now());
        var createMessage = (OrganiseringOpprettetEventData) createRecord.value().getData();
        var createMessageNew = (OrganiseringOpprettetEventData) newCreateRecord.value().getData();
        assertThat(createMessage.id()).isEqualTo(createMessageNew.id());
    }

    @Test
    @DisplayName("Endring av en organisering avslutter eksisterende og oppretter ny")
    void endring_av_en_organisering() throws JsonProcessingException {

        // Receive organisering from DVH
        var dvhOrganisering = DvhOrganisering.builder()
            .agressoIdUnder("1")
            .navnUnder("Navn_1")
            .orgNivUnder("Niva_1")
            .agressoIdOver("2")
            .navnOver("Navn_2")
            .orgNivOver("Niva_2")
            .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(dvhOrganisering));

        // Mock OrgenhetService where we find NOM id
        var orgEnhetState = mockOrgenhetServiceGetEnhet("mock_id", dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder()
        );

        var orgEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver()
        );

        // Import organiseringer from DVH
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // Organisering is now available from OrganiseringStateRepository
        OrganiseringState organiseringState = getOrganiseringState("organisering_1", orgEnhetState, orgEnhetStateOver, LocalDate.now(), null);
        organiseringStateRepository.update(organiseringState);

        // Change organisering from DVH
        var dvhOrganisering_modified = dvhOrganisering.toBuilder()
            .agressoIdOver("4000000")
            .navnOver("Navn_4000000")
            .orgNivOver("Niva_4000000")
            .build();

        mockOrgenhetServiceGetEnhet("mock_id_over_modified", dvhOrganisering_modified.getAgressoIdOver(),
            dvhOrganisering_modified.getOrgNivOver(), dvhOrganisering_modified.getNavnOver()
        );

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(dvhOrganisering_modified));

        // Import organiseringer from DVH again
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // Expect one create message and then one terminate + one create message for the change in organisering
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 3);

        var createRecord = recordsQueue.remove();
        var terminateRecord = recordsQueue.remove();
        var newCreateRecord = recordsQueue.remove();
        assertThat(createRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        assertThat(terminateRecord.value().getData()).isExactlyInstanceOf(OrganiseringAvsluttetEventData.class);
        assertThat(newCreateRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
    }

    @Test
    @DisplayName("Endringer av organiseringer")
    void endringer_av_organiseringer() throws JsonProcessingException {

        // Initial configuration with node 1 as parent for node 3, and node 3 as parent for 2
        var organisering3 = DvhOrganisering.builder()
            .agressoIdUnder("3")
            .navnUnder("Navn_3")
            .orgNivUnder("Niva_3")
            .agressoIdOver("1")
            .navnOver("Navn_1")
            .orgNivOver("Niva_1")
            .build();

        var organisering2 = DvhOrganisering.builder()
            .agressoIdUnder("2")
            .navnUnder("Navn_2")
            .orgNivUnder("Niva_2")
            .agressoIdOver("3")
            .navnOver("Navn_3")
            .orgNivOver("Niva_3")
            .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(organisering2, organisering3));

        // Setup response from orgEnhet state lookup where we find NOM id
        var overEnhetTilOrgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_1", organisering3.getAgressoIdOver(),
            organisering3.getOrgNivOver(), organisering3.getNavnOver()
        );

        var orgEnhet2 = mockOrgenhetServiceGetEnhet("nom_id_2", organisering2.getAgressoIdUnder(),
            organisering2.getOrgNivUnder(), organisering2.getNavnUnder()
        );

        var orgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_3", organisering3.getAgressoIdUnder(),
            organisering3.getOrgNivUnder(), organisering3.getNavnUnder()
        );

        // Get organiseringer from DVH, should generate two opprettet messages (organisering 2 + 3)
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // oppdatere organiseringByFid map i OrganiseringStateRepository
        OrganiseringState organiseringState3 = getOrganiseringState("organisering_3", orgEnhet3, overEnhetTilOrgEnhet3, LocalDate.now().minusDays(1), null);

        organiseringStateRepository.update(organiseringState3);

        OrganiseringState organiseringState2 = getOrganiseringState("organisering_2", orgEnhet2.id(), organisering2.getNavnUnder(), orgEnhet3.id(),
            organisering2.getNavnOver(), LocalDate.now().minusDays(1), null);
        organiseringStateRepository.update(organiseringState2);

        // New node 4 gets 3 as parent and node 2 is moved and gets 4 as new parent
        var organisering4 = DvhOrganisering.builder()
            .agressoIdUnder("4")
            .navnUnder("Navn_4")
            .orgNivUnder("Niva_4")
            .agressoIdOver("3")
            .navnOver("Navn_3")
            .orgNivOver("Niva_3")
            .build();

        var organisering2_modified = organisering2.toBuilder()
            .agressoIdOver("4")
            .navnOver("Navn_4")
            .orgNivOver("Niva_4")
            .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(organisering3, organisering2_modified, organisering4));

        var orgEnhetState4 = mockOrgenhetServiceGetEnhet("nom_id_4", organisering4.getAgressoIdUnder(),
            organisering4.getOrgNivUnder(), organisering4.getNavnUnder()
        );

        // Get organiseringer from DVH again
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // Wait until 5 messages have arrived
        // 2 messages initially + 3 from second part (organisering 4 created, old organisering 2 is terminated and new organisering 2 is created)
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 5);

        var initialNode2Record = recordsQueue.remove();
        recordsQueue.remove(); // Not checking second create message
        var terminateNode2Record = recordsQueue.remove();
        var replacementNode2Record = recordsQueue.remove();

        // Check that the expected events were received
        assertThat(initialNode2Record.value()).isExactlyInstanceOf(OrganiseringOpprettetEventV1.class);
        assertThat(terminateNode2Record.value()).isExactlyInstanceOf(OrganiseringAvsluttetEventV1.class);
        assertThat(replacementNode2Record.value()).isExactlyInstanceOf(OrganiseringOpprettetEventV1.class);


        var initialNode2Message = (OrganiseringOpprettetEventData) initialNode2Record.value().getData();
        var replacementNode2Message = (OrganiseringOpprettetEventData) replacementNode2Record.value().getData();
        var terminateNode2Message = (OrganiseringAvsluttetEventData) terminateNode2Record.value().getData();

        // Check that node 2 has the new organisering
        assertThat(replacementNode2Message.id()).isEqualTo(initialNode2Message.id());
        assertThat(replacementNode2Message.navn()).isEqualTo(initialNode2Message.navn());
        assertThat(replacementNode2Message.idOver()).isEqualTo(orgEnhetState4.id());
        assertThat(replacementNode2Message.navnOver()).isEqualTo(organisering2_modified.getNavnOver());

        // Check that the initial node 2 organisering has ended
        assertThat(terminateNode2Message.gyldigTom()).isEqualTo(LocalDate.now().minusDays(1));
    }

    @Test
    @DisplayName("Avslutt en eksisterende organisering og oppretter ny når id endret")
    void avslutt_en_organisering_og_oppretter_ny() throws JsonProcessingException {
        // node 1 som parent for node 3
        var organisering3 = DvhOrganisering.builder()
            .agressoIdUnder("3")
            .navnUnder("Navn_3")
            .orgNivUnder("Niva_3")
            .agressoIdOver("1")
            .navnOver("Navn_1")
            .orgNivOver("Niva_1")
            .build();

        // stub organisering3 fra DVH
        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(organisering3));

        // mock orgEnhetService hvor vi finner NOM id
        var overEnhetTilOrgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_1", organisering3.getAgressoIdOver(),
            organisering3.getOrgNivOver(), organisering3.getNavnOver()
        );

        var orgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_3", organisering3.getAgressoIdUnder(),
            organisering3.getOrgNivUnder(), organisering3.getNavnUnder()
        );

        // opprette organisering (1 - 3)
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // oppdatere state
        OrganiseringState organiseringState3 = getOrganiseringState("organisering_3", orgEnhet3.id(), organisering3.getNavnUnder(),
            overEnhetTilOrgEnhet3.id(), organisering3.getNavnOver(), LocalDate.now().minusDays(1), null);
        organiseringStateRepository.update(organiseringState3);


        // simulerer at id endret -  f eks navn på orgEnhet3 modifisert
        var organisering3_modified = DvhOrganisering.builder()
            .agressoIdUnder("3")
            .navnUnder("Navn_3_modified")
            .orgNivUnder("Niva_3")
            .agressoIdOver("1")
            .navnOver("Navn_1")
            .orgNivOver("Niva_1")
            .build();

        // da kommer bare organisering3_modified fra DVH
        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(organisering3_modified));

        // mock orgEnhetService hvor vi finner NOM id
        var orgEnhet3_modified = mockOrgenhetServiceGetEnhet("nom_id_3_modified", organisering3_modified.getAgressoIdUnder(),
            organisering3_modified.getOrgNivUnder(), organisering3_modified.getNavnUnder()
        );

        // hente organiseringer from DVH igjen
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // vente til meldinger
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 3);

        var createOrganisering3Record = recordsQueue.remove();
        var createOrganisering3modifiedRecord = recordsQueue.remove();
        var terminateOrganisering3Record = recordsQueue.remove();

        // assert event typer
        assertThat(createOrganisering3Record.value()).isExactlyInstanceOf(OrganiseringOpprettetEventV1.class);
        assertThat(createOrganisering3modifiedRecord.value()).isExactlyInstanceOf(OrganiseringOpprettetEventV1.class);
        assertThat(terminateOrganisering3Record.value()).isExactlyInstanceOf(OrganiseringAvsluttetEventV1.class);

        // assert verdier
        var organisering3Message = (OrganiseringOpprettetEventData) createOrganisering3Record.value().getData();
        var organisering3modifiedMessage = (OrganiseringOpprettetEventData) createOrganisering3modifiedRecord.value().getData();
        var organisering3terminateMessage = (OrganiseringAvsluttetEventData) terminateOrganisering3Record.value().getData();
        assertThat(organisering3Message.id()).isNotEqualTo(organisering3modifiedMessage.id());
        assertThat(organisering3Message.idOver()).isEqualTo(organisering3modifiedMessage.idOver());
        assertThat(organisering3Message.id()).isEqualTo(orgEnhet3.id());
        assertThat(organisering3modifiedMessage.id()).isEqualTo(orgEnhet3_modified.id());
        assertThat(organisering3terminateMessage.gyldigTom()).isEqualTo(LocalDate.now().minusDays(1));
    }

    //TODO test case for bare Abslutte en organisering
    @Test
    @DisplayName("Avslutt en eksisterende organisering")
    void avslutt_en_organisering() throws JsonProcessingException {
        // 1. Receive organisering from DVH
        var mockedOrganisering = DvhOrganisering.builder()
            .agressoIdUnder("1")
            .navnUnder("Navn_1")
            .orgNivUnder("Niva_1")
            .agressoIdOver("2")
            .navnOver("Navn_2")
            .orgNivOver("Niva_2")
            .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(mockedOrganisering));

        // Mock OrgenhetService where we find NOM id
        var orgeEnhetState = mockOrgenhetServiceGetEnhet("mock_id", mockedOrganisering.getAgressoIdUnder(),
            mockedOrganisering.getOrgNivUnder(), mockedOrganisering.getNavnUnder()
        );

        var orgeEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", mockedOrganisering.getAgressoIdOver(),
            mockedOrganisering.getOrgNivOver(), mockedOrganisering.getNavnOver()
        );

        // First import from DVH, creates the organisering
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // The organisering is now present in the state
        String fid_organisering1 = "organisering_1";
        OrganiseringState organiseringState = getOrganiseringState(fid_organisering1, orgeEnhetState, orgeEnhetStateOver, LocalDate.now(), null);
        organiseringStateRepository.update(organiseringState);

        // 2. eksisterende organisering avsluttes og ikke kommer fra DVH
        var mockedOrganisering_new = DvhOrganisering.builder()
            .agressoIdUnder("3")
            .navnUnder("Navn_3")
            .orgNivUnder("Niva_3")
            .agressoIdOver("4")
            .navnOver("Navn_4")
            .orgNivOver("Niva_4")
            .build();

        when(dvhOrdsService.getOrganiseringer()).thenReturn(List.of(mockedOrganisering_new));

        // Mock OrgenhetService where we find NOM id
        mockOrgenhetServiceGetEnhet("mock_id_new", mockedOrganisering_new.getAgressoIdUnder(),
            mockedOrganisering_new.getOrgNivUnder(), mockedOrganisering_new.getNavnUnder()
        );

        mockOrgenhetServiceGetEnhet("mock_id_over_new", mockedOrganisering_new.getAgressoIdOver(),
            mockedOrganisering_new.getOrgNivOver(), mockedOrganisering_new.getNavnOver()
        );

        // Second import from DVH, creates the organisering and terminates the old one
        dvhRestConsumer.loadOrganiseringerFromDvh();

        // Expect two create messages and then one terminate message
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 3);

        // assert event types
        var createOrganiseringRecord = recordsQueue.remove();
        var newCreateNewOrganiseringRecord = recordsQueue.remove();
        var terminateFirstOrganiseringRecord = recordsQueue.remove();
        assertThat(createOrganiseringRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        assertThat(newCreateNewOrganiseringRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        assertThat(terminateFirstOrganiseringRecord.value().getData()).isExactlyInstanceOf(OrganiseringAvsluttetEventData.class);

        // assert values og metadata
        var terminateMessage = (OrganiseringAvsluttetEventData) terminateFirstOrganiseringRecord.value().getData();
        assertThat(terminateMessage.gyldigTom()).isEqualTo(LocalDate.now());
        var terminateMessageMetadata = terminateFirstOrganiseringRecord.value().getMetadata();
        var rawData = terminateMessageMetadata.inputMetadata().inputInterfaceMethodArguments();
        assertThat(((LinkedHashMap<?, ?>) rawData).get("fid")).isEqualTo(fid_organisering1);
        assertThat(((LinkedHashMap<?, ?>) rawData).get("id")).isEqualTo(orgeEnhetState.id());
    }


    private OrgEnhetState mockOrgenhetServiceGetEnhet(String id, String kode, String niva, String navn) {
        var orgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .id(id)
            .navn(navn)
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(kode, niva, navn))
            .thenReturn(Optional.of(new OrgEnhet(orgEnhetState)));

        return orgEnhetState;
    }

    private static OrganiseringState getOrganiseringState(String fid, OrgEnhetState orgeEnhetState, OrgEnhetState orgeEnhetStateOver, LocalDate gyldigFom, LocalDate gyldigTom) {
        return getOrganiseringState(fid, orgeEnhetState.id(), orgeEnhetState.navn(), orgeEnhetStateOver.id(), orgeEnhetStateOver.navn(), gyldigFom, gyldigTom);
    }

    private static OrganiseringState getOrganiseringState(String fid, String id, String navn, String idOver, String navnOver, LocalDate gyldigFom, LocalDate gyldigTom) {
        return OrganiseringState.builder()
            .fid(fid)
            .id(id)
            .navn(navn)
            .idOver(idOver)
            .navnOver(navnOver)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .build();
    }

}
