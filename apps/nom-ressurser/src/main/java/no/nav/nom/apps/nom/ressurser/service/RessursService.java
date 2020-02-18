package no.nav.nom.apps.nom.ressurser.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.kafka.producer.KafkaProducer;
import no.nav.nom.apps.nom.ressurser.repository.RessursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Service
@AllArgsConstructor
@Slf4j
public class RessursService {

    private final RessursRepository ressursRepository;

    private final KafkaProducer kafkaProducer;

    @Transactional
    public void behandleRessursHendelse(Ressurs nyRessursHendelse){
        /*Ressurs eksisterendeRessurs = ressursRepository.findBypersonident(nyRessursHendelse.getPersonident());
        if(eksisterendeRessurs != null){
            nyRessursHendelse.setId(eksisterendeRessurs.getId());
            nyRessursHendelse.setFunksjonellId(eksisterendeRessurs.getFunksjonellId());
            nyRessursHendelse.setOpprettetDato(eksisterendeRessurs.getOpprettetDato());

        }else {
            nyRessursHendelse.setOpprettetDato(LocalDate.now());
        }
        nyRessursHendelse.setEndretDato(LocalDate.now());
        Ressurs lagretRessurs = ressursRepository.save(nyRessursHendelse);*/
        Ressurs lagretRessurs = nyRessursHendelse; //TEMP

        kafkaProducer.sendRessursMelding(RessursMapper.RessursToRessursDto(lagretRessurs));
        log.info("Ny transaksjons hendelse for funksjonell ID: ", lagretRessurs.getFunksjonellId());
    }
}

