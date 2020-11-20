package no.nav.nom.apps.dvh.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.dvh.common.AppProperties;
import no.nav.nom.apps.dvh.state.StateRepository;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Ressurs;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Component
@RequiredArgsConstructor
class RessursDvhTask {
    private final long ONE_HOUR = 60 * 60 * 1000;
    private final long FIVE_MINS = 5 * 60 * 1000;

    private final RestTemplate restTemplate;
    private final AppProperties properties;
    private final StateRepository stateRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(initialDelay = FIVE_MINS, fixedDelay = ONE_HOUR)
    public void perform() {
        var isFinishedReading = false;

        while (!isFinishedReading) {
            val offset = stateRepository.findSequenceFor(Ressurs.TYPE).orElse(0L);
            val ressurserAsJsonNodes = findRessurser(offset);

            if (!ressurserAsJsonNodes.isEmpty()) {
                val maxOffset = findMaxOffset(ressurserAsJsonNodes);
                publishToKafka(ressurserAsJsonNodes);
                stateRepository.saveOffset(Ressurs.TYPE, maxOffset);
            }
            if (ressurserAsJsonNodes.size() < properties.getDvh().getRessurs().getChunkSize()) {
                isFinishedReading = true;
            }
        }
    }

    private List<JsonNode> findRessurser(long offset) {
        try {
            val url = constructUrl(offset);
            val result = restTemplate.exchange(url, HttpMethod.GET, null, JsonNode.class);
            return findRessurser(result);
        } catch (RuntimeException e) {
            log.error("Failed to fetch ressurser from dvh", e);
            return emptyList();
        }
    }

    private String constructUrl(long offset) {
        return UriComponentsBuilder
                .fromUriString(properties.getDvh().getRessurs().getUrl())
                .queryParam("in_sekvensnummer_fra", offset)
                .queryParam("in_sekvensnummer_til", offset+properties.getDvh().getRessurs().getChunkSize())
                .build()
                .toUriString();
    }

    private List<JsonNode> findRessurser(ResponseEntity<JsonNode> result) {
        return StreamSupport.stream(result.getBody().get("Nom_AnsatteResponse").get("Nom_AnsatteResult").spliterator(), false)
                .sorted(Comparator.comparingLong(jsonRessurs -> jsonRessurs.get("SEKVENSNR").asLong()))
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
                            .type(Ressurs.TYPE)
                            .data(e)
                            .build())
                    .map(e -> {
                        val asJson = toJson(e);
                        return new ProducerRecord<>(properties.getDvh().getTopic(), "Ressurs-" + e.getOffset(), asJson);
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
