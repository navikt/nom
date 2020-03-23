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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, RessursDto> kafkaTemplate;
    private final RessursRepository repository;

    @Value("${app.kafka.topic.ressurs.name}")
    private String ressursTopic;

    @Transactional(value = "transactionManager")
    @Timed(value = "kafka.messages.produced.timed", description = "Time spent sending kafka messages", extraTags = {"topic", "aapen-nom-ressurs-v1"})
    public void send(List<Ressurs> ressursList) {
        ressursList.forEach(ressurs -> {
            kafkaTemplate.send(ressursTopic, ressurs.getNavIdent(), converted(ressurs));
            ressurs.setProduced(true);
            meterRegistry.counter("kafka.messages.produced", "topic", ressursTopic).increment();
            log.info("Sendt ressurs med navident: {}", ressurs.getNavIdent());
        });

        repository.saveAll(ressursList);
    }

    private static RessursDto converted(Ressurs ressurs) {
        return RessursDto.builder()
                .personident(ressurs.getPersonident())
                .fornavn(ressurs.getFornavn())
                .etternavn(ressurs.getEtternavn())
                .ressurstype(ressurs.getRessurstype())
                .startdato(ressurs.getStartdato())
                .sluttdato(ressurs.getSluttdato())
                .build();
    }
}
