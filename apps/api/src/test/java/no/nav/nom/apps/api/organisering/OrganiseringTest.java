package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
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
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class OrganiseringTest extends TestBase {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    OrganiseringEventCommandService organiseringEventCommandService;

    @Test
    @DisplayName("Innlesing av organiseringer")
    void innlesing_av_organiseringer() throws JsonProcessingException {

        // make mock response
        var mockedOrganisering1 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("1111111")
            .ORGNODE_NAVN("Navn_1111111")
            .ORGNODE_KODE_OVER("01111111")
            .ORGNODE_NAVN_OVER("Navn_0111111")
            .ORGNODE_NIVA("Niva_111111")
            .ORGNODE_NIVA_OVER("Niva_0111111")
            .build();
        var mockedOrganisering2 = DvhOrganiseringMock.getBuilderWithDefault().ORGNODE_KODE("2222222").build();
        var mockedOrganisering3 = DvhOrganiseringMock.getBuilderWithDefault().ORGNODE_KODE("3333333").build();

        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering1, mockedOrganisering2, mockedOrganisering3);

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var jsonDeserializer = new JsonDeserializer<>(OrganiseringOpprettetEventV1.class, objectMapper);
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("consumerGroup", "false", embeddedKafkaBroker));
        Consumer<String, OrganiseringOpprettetEventV1> consumer = new DefaultKafkaConsumerFactory<>(
            configs,
            new StringDeserializer(),
            jsonDeserializer
        ).createConsumer();
        consumer.subscribe(singleton("org.nom.organisering-eventsource"));

        ConsumerRecords<String, OrganiseringOpprettetEventV1> records = KafkaTestUtils.getRecords(consumer);

        assertThat(records.count()).isEqualTo(3);

        var receivedRecords = records.records("org.nom.organisering-eventsource");
        List<ConsumerRecord<String, OrganiseringOpprettetEventV1>> consumerRecords = new ArrayList<>();
        receivedRecords.forEach(consumerRecords::add);

        assertThat(consumerRecords.size()).isEqualTo(3);
        var consumerRecord0 = consumerRecords.get(0);

        var data = (OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData) consumerRecord0.value().getData();
        assertThat(data.agressoIdOver()).isEqualTo(mockedOrganisering1.getORGNODE_KODE_OVER());
        assertThat(data.agressoIdUnder()).isEqualTo(mockedOrganisering1.getORGNODE_KODE());
        assertThat(data.navnUnder()).isEqualTo(mockedOrganisering1.getORGNODE_NAVN());
        assertThat(data.orgNivOver()).isEqualTo(mockedOrganisering1.getORGNODE_NIVA_OVER());
        assertThat(data.orgNivUnder()).isEqualTo(mockedOrganisering1.getORGNODE_NIVA());
    }
}
