package no.nav.nom.apps.nom.ressurser.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.producer.KafkaProducer;
import no.nav.nom.apps.nom.ressurser.repository.RessursRepository;
import no.nav.nom.contract.nom.dto.RessursDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@AllArgsConstructor
@Slf4j
public class RessursService {

    private final RessursRepository repository;
    private final KafkaProducer kafka;

    @Transactional
    public Ressurs saveAndSend(Ressurs nyRessurs) {
        val ressurs = repository.findByPersonident(nyRessurs.getPersonident())
                .map(eksisterende -> {
                    nyRessurs.setId(eksisterende.getId());
                    nyRessurs.setFunksjonellId(eksisterende.getFunksjonellId());
                    nyRessurs.setOpprettetDato(eksisterende.getOpprettetDato());
                    nyRessurs.setEndretDato(LocalDate.now());
                    return repository.save(nyRessurs);
                })
                .orElseGet(() -> {
                    nyRessurs.setFunksjonellId(repository.nextFunctionalId());
                    nyRessurs.setOpprettetDato(LocalDate.now());
                    nyRessurs.setEndretDato(LocalDate.now());
                    return repository.save(nyRessurs);
                });

        kafka.send(converted(ressurs));
        log.info("Lagret og sendt ressurs med funksjonell ID: {}", ressurs.getFunksjonellId());
        return ressurs;
    }

    private static RessursDto converted(Ressurs ressurs){
        return RessursDto.builder()
                .funksjonellId(ressurs.getFunksjonellId())
                .personident(ressurs.getPersonident())
                .ressurstype(ressurs.getRessurstype())
                .startdato(ressurs.getStartdato())
                .sluttdato(ressurs.getSluttdato())
                .build();
    }
}

