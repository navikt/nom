package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhStateListener;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DisplayName("GraphQL.HentEnhet")
class OrganisasjonsenhetTest extends OrganisasjonsenhetTestBase {

    @Autowired
    private DvhStateListener dvhStateListener;

    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql med id parameter")
    @Test
    void scenario11(){
        val id = "testId";
        val navn = "testNavn";
        val testDato = LocalDate.now();

        sendFromKafkaMock(
                List.of(
                        Enhet.builder()
                                .enhetKode(id)
                                .enhetNavn(navn)
                                .enhetType("NAVENHET")
                                .enhetGyldigFra(testDato)
                                .enhetGyldigTil(testDato.plusMonths(1))
                                .build()
                )
        );

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario1.graphql");
        assertTrue(response.isOk());

        val organisasjonsEnhet = response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class).get(0);
        assertEquals(id, organisasjonsEnhet.getId());
        assertEquals(navn, organisasjonsEnhet.getNavn());
        assertEquals(testDato, organisasjonsEnhet.getGyldigFom());
        assertEquals(testDato.plusMonths(1), organisasjonsEnhet.getGyldigTil());
    }

    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql uten id parameter")
    @Test
    void scenario12() throws Exception {
        val id = "testId";
        val navn = "testNavn";
        val testDato = LocalDate.now();

        sendFromKafkaMock(
                List.of(
                        Enhet.builder()
                                .enhetKode(id)
                                .enhetNavn(navn)
                                .enhetType("NAVENHET")
                                .enhetGyldigFra(testDato)
                                .enhetGyldigTil(testDato.plusMonths(1))
                                .build(),
                        Enhet.builder()
                                .enhetKode(id+"2")
                                .enhetNavn(navn+"2")
                                .enhetType("NAVENHET")
                                .enhetGyldigFra(testDato.minusMonths(1))
                                .enhetGyldigTil(testDato.plusMonths(2))
                                .build()
                )
        );

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario2.graphql");
        assertTrue(response.isOk());
        assertEquals(2, response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class).size());
    }

    private void sendFromKafkaMock(List<Object> objectList){

        val consumer = mock(Consumer.class);
        doReturn(Map.of(new TopicPartition("", 0), 0L)).when(consumer).endOffsets(any());
        doReturn(0L).when(consumer).position(any());

        val recordsAsJson = createConsumerRecords(objectList.stream()
                        .map(e -> {
                            return DvhEvent.builder()
                                    .type(Enhet.TYPE)
                                    .data(toJsonNode(e))
                                    .build();
                        })
                        .map(this::toJson)
                        .map(e -> createConsumerRecord("", e))
                        .collect(Collectors.toUnmodifiableList()));

        dvhStateListener.onMessage(recordsAsJson, consumer);
    }

    public <K, V> ConsumerRecord<K, V> createConsumerRecord(K key, V value) {
        return new ConsumerRecord<>("", 0, 0, key, value);
    }


    public <K, V>ConsumerRecords<K, V> createConsumerRecords(List<ConsumerRecord<K, V>> records) {
        return new ConsumerRecords<>(Map.of(new TopicPartition("test", 123), records));
    }

    private JsonNode toJsonNode(Object object){
        return objectMapper.convertValue(object, JsonNode.class);
    }

    @SneakyThrows
    private String toJson(Object object){
        return objectMapper.writeValueAsString(object);
    }
}

