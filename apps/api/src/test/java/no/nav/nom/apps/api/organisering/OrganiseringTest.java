package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singleton;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrganiseringTest extends TestBase {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @MockBean
    private OrgenhetService orgenhetService;

    @BeforeEach
    public void setUp() {
        dvhOrganiseringMockUtils.clearResponse();
    }

    @Test
    @DisplayName("Innlesing av en organisering")
    void innlesing_av_en_organisering() throws JsonProcessingException {

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
        var orgeEnhetStateUnder = mockOrgEnhetSeviceGetEnhetUnder("mock_id_under", mockedOrganisering);
        var orgeEnhetStateOver = mockOrgEnhetSeviceGetEnhetOver("mock_id_over", mockedOrganisering);

        // load records from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var jsonDeserializer = new JsonDeserializer<>(OrganiseringEvent.class, objectMapper);
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("consumerGroup", "false", embeddedKafkaBroker));
        Consumer<String, OrganiseringEvent> kafkaConsumer = new DefaultKafkaConsumerFactory<>(
            configs,
            new StringDeserializer(),
            jsonDeserializer
        ).createConsumer();
        kafkaConsumer.subscribe(singleton("org.nom.organisering-eventsource"));
        ConsumerRecords<String, OrganiseringEvent> records = KafkaTestUtils.getRecords(kafkaConsumer);

        var receivedRecords = records.records("org.nom.organisering-eventsource");
        List<ConsumerRecord<String, OrganiseringEvent>> consumerRecords = new ArrayList<>();
        receivedRecords.forEach(consumerRecords::add);
        assertThat(consumerRecords.size()).isEqualTo(1);

        // Assert record
        var consumerRecord = consumerRecords.get(0);
        assertThat(consumerRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData.class);
        var eventData = (OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData) consumerRecord.value().getData();
        assertThat(eventData.idOver()).isEqualTo(orgeEnhetStateOver.id());
        assertThat(eventData.navnOver()).isEqualTo(mockedOrganisering.getORGNODE_NAVN_OVER());
        assertThat(eventData.idUnder()).isEqualTo(orgeEnhetStateUnder.id());
        assertThat(eventData.navnUnder()).isEqualTo(mockedOrganisering.getORGNODE_NAVN());
    }

    @Test
    @DisplayName("Endring av organisering")
    void endring_av_organisering() throws JsonProcessingException {

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
        var orgeEnhetStateUnder = mockOrgEnhetSeviceGetEnhetUnder("mock_id_under", mockedOrganisering);
        var orgeEnhetStateOver = mockOrgEnhetSeviceGetEnhetOver("mock_id_over", mockedOrganisering);

        // load records from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // change organisering
        var mockedOrganisering_modified = mockedOrganisering.toBuilder()
            .ORGNODE_KODE_OVER("4000000")
            .ORGNODE_NAVN_OVER("Navn_4000000")
            .ORGNODE_NIVA_OVER("Niva_4000000")
            .build();

        var orgeEnhetStateOver_modified = mockOrgEnhetSeviceGetEnhetOver("mock_id_over_modified", mockedOrganisering_modified);

        dvhOrganiseringMockUtils.clearResponse();
        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering_modified);

        // load changed records
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var jsonDeserializer = new JsonDeserializer<>(OrganiseringEvent.class, objectMapper);
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("consumerGroup", "false", embeddedKafkaBroker));

        Consumer<String, OrganiseringEvent> consumer = new DefaultKafkaConsumerFactory<>(
            configs,
            new StringDeserializer(),
            jsonDeserializer
        ).createConsumer();
        consumer.subscribe(singleton("org.nom.organisering-eventsource"));

        ConsumerRecords<String, OrganiseringEvent> records = KafkaTestUtils.getRecords(consumer);

        var receivedRecords = records.records("org.nom.organisering-eventsource");
        List<ConsumerRecord<String, OrganiseringEvent>> consumerRecords = new ArrayList<>();
        receivedRecords.forEach(consumerRecords::add);
        assertThat(consumerRecords.size()).isEqualTo(2);

        // assert that first record is OrganiseringOpprettetEventV1
        var opprettetConsumerRecord = consumerRecords.get(0);
        assertThat(opprettetConsumerRecord.value().getData()).isExactlyInstanceOf(OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData.class);

        // assert mockedOrganisering
        var opprettetEventData = (OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData) opprettetConsumerRecord.value().getData();
        assertThat(opprettetEventData.idOver()).isEqualTo(orgeEnhetStateOver.id());
        assertThat(opprettetEventData.navnOver()).isEqualTo(mockedOrganisering.getORGNODE_NAVN_OVER());
        assertThat(opprettetEventData.idUnder()).isEqualTo(orgeEnhetStateUnder.id());
        assertThat(opprettetEventData.navnUnder()).isEqualTo(mockedOrganisering.getORGNODE_NAVN());

        // assert modified mockedOrganisering
        var endretConsumerRecord = consumerRecords.get(1);
        assertThat(endretConsumerRecord.value().getData()).isExactlyInstanceOf(OrganiseringEndretEventV1.OrganiseringEndretEventData.class);
        var endretEventData = (OrganiseringEndretEventV1.OrganiseringEndretEventData) endretConsumerRecord.value().getData();
        assertThat(endretEventData.navnOver()).isEqualTo(mockedOrganisering_modified.getORGNODE_NAVN_OVER());
        assertThat(endretEventData.idOver()).isEqualTo(orgeEnhetStateOver_modified.id());
    }

    @Test
    @DisplayName("Endringer av organiseringer")
    void endringer_av_organiseringer() throws JsonProcessingException {
        // 0. [(1), 2+]
        // 1. [(1), 3+, 2*]
        // 2. [(1), 3, 4+, 2*]
        // + means opprettet, * means endret

        var mockedOrganisering2 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("2000000")
            .ORGNODE_NAVN("Navn_2000000")
            .ORGNODE_NIVA("Niva_2000000")
            .ORGNODE_KODE_OVER("3000000")
            .ORGNODE_NAVN_OVER("Navn_3000000")
            .ORGNODE_NIVA_OVER("Niva_3000000")
            .build();

        var mockedOrganisering3 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("3000000")
            .ORGNODE_NAVN("Navn_3000000")
            .ORGNODE_NIVA("Niva_3000000")
            .ORGNODE_KODE_OVER("1000000")
            .ORGNODE_NAVN_OVER("Navn_1000000")
            .ORGNODE_NIVA_OVER("Niva_1000000")
            .build();

        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering2, mockedOrganisering3); // 2+
        // Mock OrgenhetService where we find NOM id
        var orgEnhetState1 = mockOrgEnhetSeviceGetEnhetOver("nom_id_1", mockedOrganisering3);
        var orgEnhetState2 = mockOrgEnhetSeviceGetEnhetUnder("nom_id_2", mockedOrganisering2);
        var orgEnhetState3 = mockOrgEnhetSeviceGetEnhetUnder("nom_id_3", mockedOrganisering3);

        // load records from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        var mockedOrganisering4 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("4000000")
            .ORGNODE_NAVN("Navn_4000000")
            .ORGNODE_NIVA("Niva_4000000")
            .ORGNODE_KODE_OVER("3000000")
            .ORGNODE_NAVN_OVER("Navn_3000000")
            .ORGNODE_NIVA_OVER("Niva_3000000")
            .build();

        var mockedOrganisering2_modified = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("2000000")
            .ORGNODE_NAVN("Navn_2000000")
            .ORGNODE_NIVA("Niva_2000000")
            .ORGNODE_KODE_OVER("4000000")
            .ORGNODE_NAVN_OVER("Navn_4000000")
            .ORGNODE_NIVA_OVER("Niva_4000000")
            .build();


        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering3, mockedOrganisering2_modified, mockedOrganisering4); //3, 2*, 4+
        var orgEnhetState4 = mockOrgEnhetSeviceGetEnhetUnder("nom_id_4", mockedOrganisering4);
        var orgEnhetState2modified = mockOrgEnhetSeviceGetEnhetUnder("nom_id_2", mockedOrganisering2_modified);

    }


    private OrgEnhetState mockOrgEnhetSeviceGetEnhetOver(String idOver, DvhOrganiseringMock mockedOrganisering1) {
        var orgeEnhetStateOver = orgEnhetStateBuilderWithDefaultValues()
            .id(idOver)
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedOrganisering1.getORGNODE_KODE_OVER(),
            mockedOrganisering1.getORGNODE_NIVA_OVER(),
            mockedOrganisering1.getORGNODE_NAVN_OVER()
        )).thenReturn(Optional.of(new OrgEnhet(orgeEnhetStateOver)));
        return orgeEnhetStateOver;
    }

    private OrgEnhetState mockOrgEnhetSeviceGetEnhetUnder(String idUnder, DvhOrganiseringMock mockedOrganisering1) {
        var orgeEnhetStateUnder = orgEnhetStateBuilderWithDefaultValues()
            .id(idUnder)
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedOrganisering1.getORGNODE_KODE(),
            mockedOrganisering1.getORGNODE_NIVA(),
            mockedOrganisering1.getORGNODE_NAVN()
        )).thenReturn(Optional.of(new OrgEnhet(orgeEnhetStateUnder)));
        return orgeEnhetStateUnder;
    }

}
