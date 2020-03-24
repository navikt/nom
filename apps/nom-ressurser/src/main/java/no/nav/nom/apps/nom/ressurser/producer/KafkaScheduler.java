package no.nav.nom.apps.nom.ressurser.producer;

import lombok.AllArgsConstructor;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("MidlertidigDisabled")
@AllArgsConstructor
public class KafkaScheduler {

    private final RessursRepository ressursRepository;
    private final KafkaProducer kafkaProducer;

    @Scheduled(fixedDelay = 5000)
    public void produce(){
        kafkaProducer.send(ressursRepository.findAllByProducedAndNavidentNotNull(false));
    }
}
