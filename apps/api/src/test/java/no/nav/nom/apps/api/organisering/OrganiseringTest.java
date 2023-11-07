package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretEventV1.OrganiseringEndretEventData;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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


    BlockingQueue<ConsumerRecord<String, OrganiseringEvent>> recordsQueue;

    KafkaMessageListenerContainer<String, OrganiseringEvent> container;


    @BeforeEach
    public void setUp() {
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

        // make mock response
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
        var orgeEnhetStateUnder = mockOrgenhetServiceGetEnhet("mock_id_under", mockedOrganisering.getORGNODE_KODE(),
            mockedOrganisering.getORGNODE_NIVA(), mockedOrganisering.getORGNODE_NAVN());

        var orgeEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", mockedOrganisering.getORGNODE_KODE_OVER(),
            mockedOrganisering.getORGNODE_NIVA_OVER(), mockedOrganisering.getORGNODE_NAVN_OVER());


        // load records from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        ConsumerRecord<String, OrganiseringEvent> singleRecord = recordsQueue.poll(250, MILLISECONDS);

        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        var eventData = (OrganiseringOpprettetEventData) singleRecord.value().getData();

        assertThat(eventData.idOver()).isEqualTo(orgeEnhetStateOver.id());
        assertThat(eventData.navnOver()).isEqualTo(mockedOrganisering.getORGNODE_NAVN_OVER());
        assertThat(eventData.idUnder()).isEqualTo(orgeEnhetStateUnder.id());
        assertThat(eventData.navnUnder()).isEqualTo(mockedOrganisering.getORGNODE_NAVN());
    }

    @Test
    @DisplayName("Endring av en organisering")
    void endring_av_en_organisering() throws JsonProcessingException, InterruptedException {

        // make mock response
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
        var orgeEnhetStateUnder = mockOrgenhetServiceGetEnhet("mock_id_under", mockedOrganisering.getORGNODE_KODE(),
            mockedOrganisering.getORGNODE_NIVA(), mockedOrganisering.getORGNODE_NAVN());

        var orgeEnhetStateOver = mockOrgenhetServiceGetEnhet("mock_id_over", mockedOrganisering.getORGNODE_KODE_OVER(),
            mockedOrganisering.getORGNODE_NIVA_OVER(), mockedOrganisering.getORGNODE_NAVN_OVER());


        // load records from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // change organisering
        var mockedOrganisering_modified = mockedOrganisering.toBuilder()
            .ORGNODE_KODE_OVER("4000000")
            .ORGNODE_NAVN_OVER("Navn_4000000")
            .ORGNODE_NIVA_OVER("Niva_4000000")
            .build();

        var orgEnhetStateOver_modified = mockOrgenhetServiceGetEnhet("mock_id_over_modified", mockedOrganisering_modified.getORGNODE_KODE_OVER(),
            mockedOrganisering_modified.getORGNODE_NIVA_OVER(), mockedOrganisering_modified.getORGNODE_NAVN_OVER());


        dvhOrganiseringMockUtils.clearResponse();
        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering_modified);

        // load changed records
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        // assert that first record is OrganiseringOpprettetEventV1
        var opprettetConsumerRecord = recordsQueue.poll(100, MILLISECONDS);
        assertThat(opprettetConsumerRecord).isNotNull();
        assertThat(opprettetConsumerRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);

        // assert mockedOrganisering
        var opprettetEventData = (OrganiseringOpprettetEventData) opprettetConsumerRecord.value().getData();
        assertThat(opprettetEventData.idOver()).isEqualTo(orgeEnhetStateOver.id());
        assertThat(opprettetEventData.navnOver()).isEqualTo(mockedOrganisering.getORGNODE_NAVN_OVER());
        assertThat(opprettetEventData.idUnder()).isEqualTo(orgeEnhetStateUnder.id());
        assertThat(opprettetEventData.navnUnder()).isEqualTo(mockedOrganisering.getORGNODE_NAVN());

        // assert modified mockedOrganisering
        var endretConsumerRecord = recordsQueue.poll(100, MILLISECONDS);
        assertThat(endretConsumerRecord).isNotNull();
        assertThat(endretConsumerRecord.value().getData()).isExactlyInstanceOf(OrganiseringEndretEventData.class);

        var endretEventData = (OrganiseringEndretEventData) endretConsumerRecord.value().getData();
        assertThat(endretEventData.navnOver()).isEqualTo(mockedOrganisering_modified.getORGNODE_NAVN_OVER());
        assertThat(endretEventData.idOver()).isEqualTo(orgEnhetStateOver_modified.id());
    }

    @Test
    @DisplayName("Endringer av organiseringer")
    void endringer_av_organiseringer() throws JsonProcessingException {
        // 0. [(1), 2+]
        // 1. [(1), 3+, 2*]
        // 2. [(1), 3, 4+, 2*]
        // + means opprettet, * means endret

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

        dvhOrganiseringMockUtils.addToResponse(organisering2, organisering3); // 2+
        // Mock OrgenhetService where we find NOM id
        var orgEnhetState1 = mockOrgenhetServiceGetEnhet("nom_id_1", organisering3.getORGNODE_KODE_OVER(),
            organisering3.getORGNODE_NIVA_OVER(), organisering3.getORGNODE_NAVN_OVER()
        );

        var orgEnhetState2 = mockOrgenhetServiceGetEnhet("nom_id_2", organisering2.getORGNODE_KODE(),
            organisering2.getORGNODE_NIVA(), organisering2.getORGNODE_NAVN()
        );

        var orgEnhetState3 = mockOrgenhetServiceGetEnhet("nom_id_3", organisering3.getORGNODE_KODE(),
            organisering3.getORGNODE_NIVA(), organisering3.getORGNODE_NAVN()
        );

        // load records from DVH, should generate two opprettet meldinger
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


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
        dvhOrganiseringMockUtils.addToResponse(organisering3, organisering2_modified, organisering4); //3, 2*, 4+
        //        var orgEnhetState4 = mockOrgenhetServiceGetEnhetUnder("nom_id_4", mockedOrganisering4);
        var orgEnhetState4 = mockOrgenhetServiceGetEnhet("nom_id_4", organisering4.getORGNODE_KODE(),
            organisering4.getORGNODE_NIVA(), organisering4.getORGNODE_NAVN()
        );


        // load records from DVH again
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // Then
        // 3: Ikke endret, ingen melding
        // 2: Endret
        // 4: Opprettet

        // 2 messages initially + 2 from second part
        //        assertThat(recordsQueue.size()).isEqualTo(4);


        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 4);

        var createRecord = recordsQueue.remove();
        recordsQueue.remove();
        var changedRecord = recordsQueue.remove();

        assertThat(createRecord.value().getFid()).isEqualTo(changedRecord.value().getFid());

        assertThat(createRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventData.class);
        assertThat(changedRecord.value().getData()).isExactlyInstanceOf(OrganiseringEndretEventData.class);

        var createMessage = (OrganiseringOpprettetEventData) createRecord.value().getData();
        var changeMessage = (OrganiseringEndretEventData) changedRecord.value().getData();

        assertThat(createMessage.idUnder()).isEqualTo(changeMessage.idUnder());
        assertThat(createMessage.navnUnder()).isEqualTo(changeMessage.navnUnder());


        assertThat(changeMessage.idOver()).isEqualTo(orgEnhetState4.id());
        assertThat(changeMessage.navnOver()).isEqualTo(organisering2_modified.getORGNODE_NAVN_OVER());

    }


    private OrgEnhetState mockOrgenhetServiceGetEnhet(String id, String kode, String niva, String navn) {
        var orgeEnhetStateOver = orgEnhetStateBuilderWithDefaultValues()
            .id(id)
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(kode, niva, navn))
            .thenReturn(Optional.of(new OrgEnhet(orgeEnhetStateOver)));

        return orgeEnhetStateOver;
    }

    private OrgEnhetState mockOrgenhetServiceGetEnhetOver(String idOver, DvhOrganiseringMock mockedOrganisering) {
        var orgeEnhetStateOver = orgEnhetStateBuilderWithDefaultValues()
            .id(idOver)
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedOrganisering.getORGNODE_KODE_OVER(),
            mockedOrganisering.getORGNODE_NIVA_OVER(),
            mockedOrganisering.getORGNODE_NAVN_OVER()
        )).thenReturn(Optional.of(new OrgEnhet(orgeEnhetStateOver)));
        return orgeEnhetStateOver;
    }

    private OrgEnhetState mockOrgenhetServiceGetEnhetUnder(String idUnder, DvhOrganiseringMock mockedOrganisering) {
        var orgeEnhetStateUnder = orgEnhetStateBuilderWithDefaultValues()
            .id(idUnder)
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedOrganisering.getORGNODE_KODE(),
            mockedOrganisering.getORGNODE_NIVA(),
            mockedOrganisering.getORGNODE_NAVN()
        )).thenReturn(Optional.of(new OrgEnhet(orgeEnhetStateUnder)));
        return orgeEnhetStateUnder;
    }

}
