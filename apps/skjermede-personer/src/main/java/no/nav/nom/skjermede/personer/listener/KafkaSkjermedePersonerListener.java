package no.nav.nom.skjermede.personer.consumer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@AllArgsConstructor
@Slf4j
public class KafkaSkjermedePersonerListener {

    private final SkjermetPersonRepository repository;

    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(RessursDto ressurs) {
        val skjermetPerson = repository.findAllByPersonident(ressurs.getPersonident())
                .map(eksisterende -> {
                    eksisterende.setSkjermetFom(ressurs.getStartdato());
                    eksisterende.setSkjermetTom(ressurs.getSluttdato());
                    eksisterende.setEndretDato(LocalDateTime.now());
                    return repository.save(eksisterende);
                });

        log.info("Ny transaksjons hendelse fra aapen-nom-ressurs-v1 for funksjonell ID: {}", ressurs.getFunksjonellId());
    }

    public static SkjermetPerson converted(RessursDto record){
        return SkjermetPerson.builder()
                .personident(record.getPersonident())
                .skjermetFom(record.getStartdato())
                .skjermetTom(record.getSluttdato())
                .tilknyttetRessurs(null)
                .build();
    }
}
