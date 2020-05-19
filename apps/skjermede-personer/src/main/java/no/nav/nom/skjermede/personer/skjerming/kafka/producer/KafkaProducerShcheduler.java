package no.nav.nom.skjermede.personer.skjerming.kafka.producer;

import lombok.AllArgsConstructor;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Profile("!test")
public class KafkaProducerShcheduler {

    private final SkjermetPersonRepository skjermetPersonRepository;
    private final SkjermedePersonerProducer kafkaProducer;

    @Scheduled(fixedDelay = 5000)
    public void produce(){
        kafkaProducer.send(skjermetPersonRepository.findAllByKafkaProduced(false));
    }
}
