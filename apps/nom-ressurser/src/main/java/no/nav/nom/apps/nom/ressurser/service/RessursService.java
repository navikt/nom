package no.nav.nom.apps.nom.ressurser.service;

import lombok.AllArgsConstructor;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.kafka.producer.KafkaProducer;
import no.nav.nom.apps.nom.ressurser.repository.RessursRepository;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;

@Service
@AllArgsConstructor
public class RessursService {

    private final RessursRepository ressursRepository;

    private final KafkaProducer kafkaProducer;

    @Transactional
    public void opprettRessurs(Ressurs nyRessurs){
        kafkaProducer.sendRessursMelding(new RessursDto("Test", Ressurstype.INTERN, LocalDate.now(), LocalDate.now(), LocalDate.now(), LocalDate.now()));
        System.out.println("Ressurs opprettet");
    }

}

