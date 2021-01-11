package no.nav.nom.apps.dvh.enhet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.dvh.common.AppProperties;
import no.nav.nom.apps.dvh.state.StateRepository;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnhetDvhTask {
    private final long ONE_HOUR = 60 * 60 * 1000;
    private final long FIVE_MINS = 5 * 60 * 1000;

    private final AppProperties properties;
    private final StateRepository stateRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final RestTemplate restTemplate;

    @Scheduled(initialDelay = FIVE_MINS, fixedDelay = ONE_HOUR)
    public void perform() {
        val offset = stateRepository.findSequenceFor(Enhet.TYPE).orElse(0L);
        val enheterAsJsonNodes = findEnheter(offset);

        if (!enheterAsJsonNodes.isEmpty()) {
            val maxOffset = findMaxOffset(enheterAsJsonNodes);
            publishToKafka(enheterAsJsonNodes);
            stateRepository.saveOffset(Enhet.TYPE, maxOffset);
            meterRegistry.counter("dvh.enheter.sent").increment(enheterAsJsonNodes.size());
            log.info("{} enheter sent to Kafka. Original offset: {}, new: {}", enheterAsJsonNodes.size(), offset, maxOffset);
        }
    }

    private List<JsonNode> findEnheter(long offset) {
        try {
            val url = constructUrl(offset);
            val result = restTemplate.exchange(url, HttpMethod.GET, null, JsonNode.class);
            return findEnheter(result);
        } catch (RuntimeException e) {
            log.error("Failed to fetch enheter from dvh", e);
            return emptyList();
        }
    }

    private String constructUrl(long offset) {
        return UriComponentsBuilder
                .fromUriString(properties.getDvh().getEnhet().getUrl())
                .queryParam("in_sekvens_fra", offset)
                .build()
                .toUriString();
    }

    private List<JsonNode> findEnheter(ResponseEntity<JsonNode> result) {
        return StreamSupport.stream(result.getBody().get("NOM_HRORG_HIEARKI_PARAMResponse").get("NOM_HRORG_HIEARKI_PARAMResult").spliterator(), false)
                .collect(toUnmodifiableList());
    }

    private long findMaxOffset(List<JsonNode> nodes) {
        return nodes.stream()
                .mapToLong(e -> e.get("SEKVENSNR").asLong())
                .max()
                .orElseThrow();
    }

    private void publishToKafka(List<JsonNode> nodes) {
        kafkaTemplate.executeInTransaction(operations -> {
            nodes.stream()
                    .map(e -> DvhEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .offset(e.get("SEKVENSNR").asLong())
                            .type(Enhet.TYPE)
                            .data(e)
                            .build())
                    .sorted((o1, o2) -> (int) (o1.getOffset() - o2.getOffset()))
                    .map(e -> {
                        val asJson = toJson(e);
                        return new ProducerRecord<>(properties.getDvh().getTopic(), "Enhet-" + e.getOffset(), asJson);
                    })
                    .forEach(operations::send);
            return null;
        });
    }

    @SneakyThrows
    private String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
