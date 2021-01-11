package no.nav.nom.apps.dvh.stilling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.dvh.common.AppProperties;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Stilling;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StillingDvhTask {
    private final long ONE_HOUR = 60 * 60 * 1000;
    private final long FIVE_MINS = 5 * 60 * 1000;

    private final AppProperties properties;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    //@Scheduled(initialDelay = FIVE_MINS, fixedDelay = ONE_HOUR)
    public void perform(){
        val stillinger = getStillingerResponse();
        publishToKafka(stillinger);
    }

    private JsonNode getStillingerResponse(){
        return restTemplate.exchange(properties.getDvh().getStilling().getUrl(),
                HttpMethod.GET, null, JsonNode.class).getBody();
    }

    private void publishToKafka(JsonNode node){
        kafkaTemplate.executeInTransaction(operations -> {
            val event = DvhEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .offset(0)
                    .type(Stilling.TYPE)
                    .data(node)
                    .build();
            operations.send(new ProducerRecord<>(properties.getDvh().getTopic(), "StillingTabell", toJson(event)));
            return null;
        });
    }

    @SneakyThrows
    private String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
