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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
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

    @Test
    @DisplayName("Innlesing av organiseringer")
    void innlesing_av_organiseringer() throws JsonProcessingException {

        // make mock response
        var mockedOrganisering1 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("1000000")
            .ORGNODE_NAVN("Navn_1000000")
            .ORGNODE_NIVA("Niva_1000000")
            .ORGNODE_KODE_OVER("2000000")
            .ORGNODE_NAVN_OVER("Navn_2000000")
            .ORGNODE_NIVA_OVER("Niva_2000000")
            .build();
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
            .build();

        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering1, mockedOrganisering2, mockedOrganisering3);

        // Mock OrgenhetService where we find NOM id
        var orgeEnhetStateUnder = orgEnhetStateBuilderWithDefaultValues()
            .id("id_under")
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedOrganisering1.getORGNODE_KODE(),
            mockedOrganisering1.getORGNODE_NIVA(),
            mockedOrganisering1.getORGNODE_NAVN()
        )).thenReturn(Optional.of(new OrgEnhet(orgeEnhetStateUnder)));

        var orgeEnhetStateOver = orgEnhetStateBuilderWithDefaultValues()
            .id("id_over")
            .build();

        when(orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedOrganisering1.getORGNODE_KODE_OVER(),
            mockedOrganisering1.getORGNODE_NIVA_OVER(),
            mockedOrganisering1.getORGNODE_NAVN_OVER()
        )).thenReturn(Optional.of(new OrgEnhet(orgeEnhetStateOver)));

        // load records from DVH
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // change organisering
        var mockedOrganisering1_modified = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("1000000")
            .ORGNODE_NAVN("Navn_1000000")
            .ORGNODE_NIVA("Niva_1000000")
            .ORGNODE_KODE_OVER("4000000")
            .ORGNODE_NAVN_OVER("Navn_4000000")
            .ORGNODE_NIVA_OVER("Niva_4000000")
            .build();
        var mockedOrganisering4 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("4000000")
            .ORGNODE_NAVN("Navn_4000000")
            .ORGNODE_NIVA("Niva_4000000")
            .ORGNODE_KODE_OVER("2000000")
            .ORGNODE_NAVN_OVER("Navn_2000000")
            .ORGNODE_NIVA_OVER("Niva_2000000")
            .build();

        dvhOrganiseringMockUtils.clearResponse();
        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering1_modified, mockedOrganisering2, mockedOrganisering3, mockedOrganisering4);

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

        assertThat(records.count()).isEqualTo(7);

        var receivedRecords = records.records("org.nom.organisering-eventsource");
        List<ConsumerRecord<String, OrganiseringEvent>> consumerRecords = new ArrayList<>();
        receivedRecords.forEach(consumerRecords::add);

        assertThat(consumerRecords.size()).isEqualTo(7);

        // Assert that first record is OrganiseringOpprettetEventV1
        var consumerRecord0 = consumerRecords.get(0);
        var eventData0 = (OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData) consumerRecord0.value().getData();
        assertThat(eventData0.idOver()).isEqualTo(orgeEnhetStateOver.id());
        assertThat(eventData0.navnOver()).isEqualTo(mockedOrganisering1.getORGNODE_NAVN_OVER());
        assertThat(eventData0.idUnder()).isEqualTo(orgeEnhetStateUnder.id());
        assertThat(eventData0.navnUnder()).isEqualTo(mockedOrganisering1.getORGNODE_NAVN());

        // Assert that fourth record is OrganiseringEndretEventV1
        var consumerRecord3 = consumerRecords.get(3);
        var eventData3 = (OrganiseringEndretEventV1.OrganiseringEndretEventData) consumerRecord3.value().getData();
        assertThat(eventData3.navnOver()).isEqualTo(mockedOrganisering4.getORGNODE_NAVN());
    }

}
