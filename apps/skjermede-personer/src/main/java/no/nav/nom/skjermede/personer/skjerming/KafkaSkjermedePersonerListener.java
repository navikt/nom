package no.nav.nom.skjermede.personer.skjerming;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
@Slf4j
public class KafkaSkjermedePersonerListener {

    private final SkjermetPersonRepository repository;

    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(RessursDto ressurs) {
        repository.save(
                repository.findAllByPersonident(ressurs.getPersonident())
                .map(eksisterende -> {
                    eksisterende.setSkjermetFom(ressurs.getStartdato());
                    eksisterende.setSkjermetTom(ressurs.getSluttdato());
                    eksisterende.setEndretDato(LocalDateTime.now());
                    log.info("Ny oppdatering fra aapen-nom-ressurs-v1 for funksjonell ID: {}", ressurs.getFunksjonellId());
                    return eksisterende;
                }).orElseGet(()->{
                    SkjermetPerson nySkjermetPerson = converted(ressurs);
                    nySkjermetPerson.setEndretDato(LocalDateTime.now());
                    nySkjermetPerson.setOpprettetDato(LocalDateTime.now());
                    log.info("Ny skjerming opprettet fra aapen-nom-ressurs-v1 for funksjonell ID: {}", ressurs.getFunksjonellId());
                    return nySkjermetPerson;
                })
        );
    }

    public static SkjermetPerson converted(RessursDto record){
        return SkjermetPerson.builder()
                .personident(record.getPersonident())
                .funksjonellId(record.getFunksjonellId())
                .skjermetFom(record.getStartdato())
                .skjermetTom(record.getSluttdato())
                .tilknyttetRessurs(null)
                .build();
    }
}
