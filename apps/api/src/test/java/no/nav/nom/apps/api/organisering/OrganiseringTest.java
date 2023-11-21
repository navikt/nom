package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrganiseringTest extends TestBase {

    private static final String TOPIC = "org.nom.organisering-eventsource";

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @MockBean
    private OrgenhetService orgenhetService;

    @Autowired
    private OrganiseringStateRepository organiseringStateRepository;


    BlockingQueue<ConsumerRecord<String, OrganiseringEvent>> recordsQueue;

    KafkaMessageListenerContainer<String, OrganiseringEvent> container;


    @BeforeEach
    public void setUp() throws JsonProcessingException {

        // Always enable features for tests
        when(unleash.isEnabled(any())).thenReturn(true);

        dvhOrganiseringMockUtils.clearResponse();

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
    }

    @AfterEach
    public void tearDown() {
        container.stop();
    }


    @Test
    @DisplayName("Opprett en organisering")
    void opprett_en_organisering() throws JsonProcessingException, InterruptedException {

        // Mock DVH API response
        var mockedOrganisering = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("1000000")
            .ORGNODE_NAVN("Navn_1000000")
            .ORGNODE_NIVA("Niva_1000000")
            .ORGNODE_KODE_OVER("2000000")
            .ORGNODE_NAVN_OVER("Navn_2000000")
            .ORGNODE_NIVA_OVER("Niva_2000000")
            .build();

        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering);

        // Mock OrgenhetService where we find NOM id
        var orgeEnhetState = mockOrgenhetServiceGetEnhet("mock_id", mockedOrganisering.getORGNODE_KODE(),
            mockedOrganisering.getORGNODE_NIVA(), mockedOrganisering.getORGNODE_NAVN()
        );

        var orgeEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", mockedOrganisering.getORGNODE_KODE_OVER(),
            mockedOrganisering.getORGNODE_NIVA_OVER(), mockedOrganisering.getORGNODE_NAVN_OVER()
        );


        // Get organiseringer from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        ConsumerRecord<String, OrganiseringEvent> singleRecord = recordsQueue.poll(250, MILLISECONDS);

        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        var eventData = (OrganiseringOpprettetEventData) singleRecord.value().getData();

        assertThat(eventData.idOver()).isEqualTo(orgeEnhetStateOver.id());
        assertThat(eventData.navnOver()).isEqualTo(mockedOrganisering.getORGNODE_NAVN_OVER());
        assertThat(eventData.id()).isEqualTo(orgeEnhetState.id());
        assertThat(eventData.navn()).isEqualTo(mockedOrganisering.getORGNODE_NAVN());
        assertThat(eventData.gyldigFom()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Avslutt en organisering som ikke lenger mottas fra DVH")
    void avslutt_en_organisering() throws JsonProcessingException {

        // Receive organisering from DVH
        var mockedOrganisering = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("1000000")
            .ORGNODE_NAVN("Navn_1000000")
            .ORGNODE_NIVA("Niva_1000000")
            .ORGNODE_KODE_OVER("2000000")
            .ORGNODE_NAVN_OVER("Navn_2000000")
            .ORGNODE_NIVA_OVER("Niva_2000000")
            .build();
        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering);

        // Mock OrgenhetService where we find NOM id
        var orgeEnhetState = mockOrgenhetServiceGetEnhet("mock_id", mockedOrganisering.getORGNODE_KODE(),
            mockedOrganisering.getORGNODE_NIVA(), mockedOrganisering.getORGNODE_NAVN()
        );

        var orgeEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", mockedOrganisering.getORGNODE_KODE_OVER(),
            mockedOrganisering.getORGNODE_NIVA_OVER(), mockedOrganisering.getORGNODE_NAVN_OVER()
        );

        // First import from DVH, creates the organisering
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        // The organisering is now present in the state
        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid("organisering_1")
            .id(orgeEnhetState.id())
            .navn(orgeEnhetState.navn())
            .idOver(orgeEnhetStateOver.id())
            .navnOver(orgeEnhetStateOver.navn())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.empty())
            .build();
        organiseringStateRepository.putEventAggregate(organiseringState);

        // No longer receive organisering from DVH
        dvhOrganiseringMockUtils.clearResponse();


        // Second import from DVH, creates avslutt organisering event
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // Expect two messages
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 2);

        var createRecord = recordsQueue.remove();
        var terminateRecord = recordsQueue.remove();
        assertThat(createRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        assertThat(terminateRecord.value().getData()).isExactlyInstanceOf(OrganiseringAvsluttetEventData.class);

        var terminateMessage = (OrganiseringAvsluttetEventData) terminateRecord.value().getData();
        assertThat(terminateMessage.gyldigTom()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Endring av en organisering avslutter eksisterende og oppretter ny")
    void endring_av_en_organisering() throws JsonProcessingException {

        // Receive organisering from DVH
        var mockedOrganisering = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("1000000")
            .ORGNODE_NAVN("Navn_1000000")
            .ORGNODE_NIVA("Niva_1000000")
            .ORGNODE_KODE_OVER("2000000")
            .ORGNODE_NAVN_OVER("Navn_2000000")
            .ORGNODE_NIVA_OVER("Niva_2000000")
            .build();

        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering);

        // Mock OrgenhetService where we find NOM id
        var orgEnhetState = mockOrgenhetServiceGetEnhet("mock_id", mockedOrganisering.getORGNODE_KODE(),
            mockedOrganisering.getORGNODE_NIVA(), mockedOrganisering.getORGNODE_NAVN()
        );

        var orgEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", mockedOrganisering.getORGNODE_KODE_OVER(),
            mockedOrganisering.getORGNODE_NIVA_OVER(), mockedOrganisering.getORGNODE_NAVN_OVER()
        );

        // Import organiseringer from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        // Organisering is now available from OrganiseringStateRepository
        String fidInitialOrganisering = "organisering_1";
        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid(fidInitialOrganisering)
            .id(orgEnhetState.id())
            .navn(orgEnhetState.navn())
            .idOver(orgEnhetStateOver.id())
            .navnOver(orgEnhetStateOver.navn())
            .build();
        organiseringStateRepository.putEventAggregate(organiseringState);


        // Change organisering from DVH
        var mockedOrganisering_modified = mockedOrganisering.toBuilder()
            .ORGNODE_KODE_OVER("4000000")
            .ORGNODE_NAVN_OVER("Navn_4000000")
            .ORGNODE_NIVA_OVER("Niva_4000000")
            .build();

        mockOrgenhetServiceGetEnhet("mock_id_over_modified", mockedOrganisering_modified.getORGNODE_KODE_OVER(),
            mockedOrganisering_modified.getORGNODE_NIVA_OVER(), mockedOrganisering_modified.getORGNODE_NAVN_OVER()
        );

        dvhOrganiseringMockUtils.clearResponse();
        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering_modified);


        // Import organiseringer from DVH again
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


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
        var organisering3 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("3")
            .ORGNODE_NAVN("Navn_3")
            .ORGNODE_NIVA("Niva_3")
            .ORGNODE_KODE_OVER("1")
            .ORGNODE_NAVN_OVER("Navn_1")
            .ORGNODE_NIVA_OVER("Niva_1")
            .build();

        var organisering2 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("2")
            .ORGNODE_NAVN("Navn_2")
            .ORGNODE_NIVA("Niva_2")
            .ORGNODE_KODE_OVER("3")
            .ORGNODE_NAVN_OVER("Navn_3")
            .ORGNODE_NIVA_OVER("Niva_3")
            .build();

        dvhOrganiseringMockUtils.addToResponse(organisering2, organisering3);


        // Setup response from orgEnhet state lookup where we find NOM id
        var overEnhetTilOrgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_1", organisering3.getORGNODE_KODE_OVER(),
            organisering3.getORGNODE_NIVA_OVER(), organisering3.getORGNODE_NAVN_OVER()
        );

        var orgEnhet2 = mockOrgenhetServiceGetEnhet("nom_id_2", organisering2.getORGNODE_KODE(),
            organisering2.getORGNODE_NIVA(), organisering2.getORGNODE_NAVN()
        );

        var orgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_3", organisering3.getORGNODE_KODE(),
            organisering3.getORGNODE_NIVA(), organisering3.getORGNODE_NAVN()
        );

        // Get organiseringer from DVH, should generate two opprettet messages (organisering 2 + 3)
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        // oppdatere organiseringByFid map i OrganiseringStateRepository
        OrganiseringState organiseringState3 = OrganiseringState.builder()
            .fid("organisering_3")
            .id(orgEnhet3.id())
            .navn(organisering3.getORGNODE_NAVN())
            .idOver(overEnhetTilOrgEnhet3.id())
            .navnOver(organisering3.getORGNODE_NAVN_OVER())
            .gyldigFom(LocalDate.now().minusDays(1))
            .gyldigTom(Optional.empty())
            .build();
        organiseringStateRepository.putEventAggregate(organiseringState3);

        OrganiseringState organiseringState2 = OrganiseringState.builder()
            .fid("organisering_2")
            .id(orgEnhet2.id())
            .navn(organisering2.getORGNODE_NAVN())
            .idOver(orgEnhet3.id())
            .navnOver(organisering2.getORGNODE_NAVN_OVER())
            .gyldigFom(LocalDate.now().minusDays(1))
            .gyldigTom(Optional.empty())
            .build();
        organiseringStateRepository.putEventAggregate(organiseringState2);

        // New node 4 gets 3 as parent and node 2 is moved and gets 4 as new parent
        var organisering4 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("4")
            .ORGNODE_NAVN("Navn_4")
            .ORGNODE_NIVA("Niva_4")
            .ORGNODE_KODE_OVER("3")
            .ORGNODE_NAVN_OVER("Navn_3")
            .ORGNODE_NIVA_OVER("Niva_3")
            .build();

        var organisering2_modified = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("2")
            .ORGNODE_NAVN("Navn_2")
            .ORGNODE_NIVA("Niva_2")
            .ORGNODE_KODE_OVER("4")
            .ORGNODE_NAVN_OVER("Navn_4")
            .ORGNODE_NIVA_OVER("Niva_4")
            .build();


        dvhOrganiseringMockUtils.clearResponse();
        dvhOrganiseringMockUtils.addToResponse(organisering3, organisering2_modified, organisering4);

        var orgEnhetState4 = mockOrgenhetServiceGetEnhet("nom_id_4", organisering4.getORGNODE_KODE(),
            organisering4.getORGNODE_NIVA(), organisering4.getORGNODE_NAVN()
        );


        // Get organiseringer from DVH again
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


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
        assertThat(replacementNode2Message.navnOver()).isEqualTo(organisering2_modified.getORGNODE_NAVN_OVER());

        // Check that the initial node 2 organisering has ended
        assertThat(terminateNode2Message.gyldigTom()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Avslutt en eksisterende organisering og oppretter ny når id endret")
    void avslutt_en_organisering_og_oppretter_ny() throws JsonProcessingException {
        // node 1 som parent for node 3
        var organisering3 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("3")
            .ORGNODE_NAVN("Navn_3")
            .ORGNODE_NIVA("Niva_3")
            .ORGNODE_KODE_OVER("1")
            .ORGNODE_NAVN_OVER("Navn_1")
            .ORGNODE_NIVA_OVER("Niva_1")
            .build();

        // stub organisering3 fra DVH
        dvhOrganiseringMockUtils.addToResponse(organisering3);

        // mock orgEnhetService hvor vi finner NOM id
        var overEnhetTilOrgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_1", organisering3.getORGNODE_KODE_OVER(),
            organisering3.getORGNODE_NIVA_OVER(), organisering3.getORGNODE_NAVN_OVER()
        );
        var orgEnhet3 = mockOrgenhetServiceGetEnhet("nom_id_3", organisering3.getORGNODE_KODE(),
            organisering3.getORGNODE_NIVA(), organisering3.getORGNODE_NAVN()
        );

        // opprette organisering (1 - 3)
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // oppdatere state
        OrganiseringState organiseringState3 = OrganiseringState.builder()
            .fid("organisering_3")
            .id(orgEnhet3.id())
            .navn(organisering3.getORGNODE_NAVN())
            .idOver(overEnhetTilOrgEnhet3.id())
            .navnOver(organisering3.getORGNODE_NAVN_OVER())
            .gyldigFom(LocalDate.now().minusDays(1))
            .gyldigTom(Optional.empty())
            .build();
        organiseringStateRepository.putEventAggregate(organiseringState3);

        dvhOrganiseringMockUtils.clearResponse();

        // simulerer at id endret -  f eks navn på orgEnhet3 modifisert
        var organisering3_modified = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("3")
            .ORGNODE_NAVN("Navn_3_modified")
            .ORGNODE_NIVA("Niva_3")
            .ORGNODE_KODE_OVER("1")
            .ORGNODE_NAVN_OVER("Navn_1")
            .ORGNODE_NIVA_OVER("Niva_1")
            .build();

        // da kommer bare organisering3_modified fra DVH
        dvhOrganiseringMockUtils.addToResponse(organisering3_modified);

        // mock orgEnhetService hvor vi finner NOM id
        var orgEnhet3_modified = mockOrgenhetServiceGetEnhet("nom_id_3_modified", organisering3_modified.getORGNODE_KODE(),
            organisering3_modified.getORGNODE_NIVA(), organisering3_modified.getORGNODE_NAVN()
        );

        // hente organiseringer from DVH igjen
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

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
        assertThat(organisering3terminateMessage.gyldigTom()).isEqualTo(LocalDate.now());
    }

    private OrgEnhetState mockOrgenhetServiceGetEnhet(String id, String kode, String niva, String navn) {
        var orgEnhetState = orgEnhetStateBuilderWithDefaultValues()
            .id(id)
            .navn(navn)
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(kode, niva, navn))
            .thenReturn(Optional.of(new OrgEnhet(orgEnhetState)));

        return orgEnhetState;
    }

}
