package no.nav.nom.skjermede.personer.skjerming;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
@Slf4j
public class KafkaSkjermedePersonerListener {

    private final SkjermetPersonRepository repository;

    @Transactional
    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(RessursDto ressurs) {
        if(ressurs.getRessurstype() == Ressurstype.INTERN) {
            repository.save(
                    repository.findAllByPersonident(ressurs.getPersonident())
                            .map(eksisterende -> {
                                eksisterende.setSkjermetFom(ressurs.getStartdato());
                                eksisterende.setSkjermetTom(ressurs.getSluttdato());
                                eksisterende.setEndretDato(LocalDateTime.now());
                                log.info("Ny oppdatering fra aapen-nom-ressurs-v1 for funksjonell ID: {}", ressurs.getNavIdent());
                                return eksisterende;
                            }).orElseGet(() -> {
                                SkjermetPerson nySkjermetPerson = converted(ressurs);
                                nySkjermetPerson.setEndretDato(LocalDateTime.now());
                                nySkjermetPerson.setOpprettetDato(LocalDateTime.now());
                                log.info("Ny skjerming opprettet fra aapen-nom-ressurs-v1 for funksjonell ID: {}", ressurs.getNavIdent());
                                return nySkjermetPerson;
                    })
            );
        }
    }

    public static SkjermetPerson converted(RessursDto record){
        return SkjermetPerson.builder()
                .personident(record.getPersonident())
                .navIdent(record.getNavIdent())
                .fornavn(record.getFornavn())
                .etternavn(record.getEtternavn())
                .skjermetFom(record.getStartdato())
                .skjermetTom(record.getSluttdato())
                .tilknyttetRessurs(null)
                .build();
    }
}
