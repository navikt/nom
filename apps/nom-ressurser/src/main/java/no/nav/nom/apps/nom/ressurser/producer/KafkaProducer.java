package no.nav.nom.apps.nom.ressurser.producer;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.RessursDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, RessursDto> kafkaTemplate;
    private final RessursRepository repository;

    @Value("${app.kafka.topic.ressurs.name}")
    private String ressursTopic;

    @Scheduled(cron = "${app.scheduledTasks.kafkaProducer}")
    public void produce(){
        repository.findAllByProduced(false).forEach(this::send);
    }

    @Transactional
    @Timed(value = "kafka.messages.produced.timed", description = "Time spent sending kafka messages", extraTags = {"topic", "aapen-nom-ressurs-v1"})
    public void send(Ressurs ressurs) {
            kafkaTemplate.send(ressursTopic, ressurs.getPersonident(), converted(ressurs));
            ressurs.setProduced(true);
            repository.save(ressurs);
            meterRegistry.counter("kafka.messages.produced", "topic", ressursTopic).increment();
            //log.info("Sendt ressurs med funksjonell ID: {}", ressurs.getFunksjonellId());
    }

    private static RessursDto converted(Ressurs ressurs) {
        return RessursDto.builder()
                //.funksjonellId(ressurs.getFunksjonellId())
                .personident(ressurs.getPersonident())
                .ressurstype(ressurs.getRessurstype())
                .startdato(ressurs.getStartdato())
                .sluttdato(ressurs.getSluttdato())
                .build();
    }
}
