package no.nav.nom.apps.nom.ressurser.service;

import lombok.AllArgsConstructor;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.kafka.producer.KafkaProducer;
import no.nav.nom.apps.nom.ressurser.repository.RessursRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RessursService {

    private final RessursRepository ressursRepository;

    private final KafkaProducer kafkaProducer;

    public void opprettRessurs(Ressurs nyRessurs){
        System.out.println("Ressurs opprettet");
        kafkaProducer.sendMelding("Ressurser", "testKey", "testBody");
    }

}

