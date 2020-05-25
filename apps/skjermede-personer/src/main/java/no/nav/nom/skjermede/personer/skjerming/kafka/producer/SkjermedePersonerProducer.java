package no.nav.nom.skjermede.personer.skjerming.kafka.producer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.skjermede.dto.SkjermetPersonDTO;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("!test")
public class SkjermedePersonerProducer {

    private final KafkaTemplate<String, SkjermetPersonDTO> kafkaTemplate;
    private final SkjermetPersonRepository skjermetPersonRepository;
    private final MeterRegistry meterRegistry;



    @Value("${app.kafka.topic.skjermet.name}")
    private String skjermetTopic;

    @Transactional(value = "transactionManager")
    public void send(List<SkjermetPerson> skjermedePersoner) {
        skjermedePersoner.sort(Comparator.comparing(SkjermetPerson::getEndretDato));
        skjermedePersoner.forEach(skjermetPerson -> {
                    kafkaTemplate.send(skjermetTopic, skjermetPerson.getPersonident(), converted(skjermetPerson));
                    skjermetPerson.setKafkaProduced(true);
                    log.info("Produsert ny hendelse ut på kafka for ID: {}", skjermetPerson.getId());
                });
        skjermetPersonRepository.saveAll(skjermedePersoner);
        meterRegistry.counter("kafka_messages_produced_count", "topic", skjermetTopic).increment(skjermedePersoner.size());
    }

    private static SkjermetPersonDTO converted(SkjermetPerson skjermetPerson) {
        return SkjermetPersonDTO.builder()
                .skjermetFra(skjermetPerson.getSkjermetFra())
                .skjermetTil(skjermetPerson.getSkjermetTil())
                .build();
    }

}
