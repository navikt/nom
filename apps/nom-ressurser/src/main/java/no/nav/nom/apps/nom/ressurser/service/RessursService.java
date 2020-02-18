package no.nav.nom.apps.nom.ressurser.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.kafka.producer.KafkaProducer;
import no.nav.nom.apps.nom.ressurser.repository.RessursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@AllArgsConstructor
@Slf4j
public class RessursService {

    private final RessursRepository ressursRepository;

    private final KafkaProducer kafkaProducer;

    @Transactional
    public void opprettRessurs(Ressurs nyRessurs){
        //Ressurs lagretRessurs = ressursRepository.save(nyRessurs);
        kafkaProducer.sendRessursMelding(RessursMapper.RessursToRessursDto(nyRessurs));
        //log.info("Ny hendelse for funksjonell ID: ", nyRessurs.getFunksjonellId());
    }

}

