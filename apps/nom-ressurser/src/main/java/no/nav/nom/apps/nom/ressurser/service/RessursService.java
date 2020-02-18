package no.nav.nom.apps.nom.ressurser.service;

import lombok.AllArgsConstructor;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.kafka.producer.KafkaProducer;
import no.nav.nom.apps.nom.ressurser.repository.RessursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@AllArgsConstructor
public class RessursService {

    private final RessursRepository ressursRepository;

    private final KafkaProducer kafkaProducer;

    @Transactional
    public void opprettRessurs(Ressurs nyRessurs){
        kafkaProducer.sendRessursMelding(RessursMapper.RessursToRessursDto(nyRessurs));
        System.out.println("Ressurs opprettet");
    }

}

