package no.nav.nom.skjermede.personer.consumer;

import lombok.AllArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Component
@AllArgsConstructor
public class SkjermedePersonerConsumer {

    private SkjermetPersonRepository repository;

    @Transactional
    public void processRecord(ConsumerRecord<String, RessursDto> record){

        RessursDto ressursDto = record.value();
        SkjermetPerson skjermetPerson = ConsumerRecordToSkjermetPersonMapper.consumerRecordToSkjermetPersonMapper(ressursDto);

        skjermetPerson.setEndretDato(LocalDateTime.now());

        if (repository.existsById(skjermetPerson.getPersonident())) {
            // Update Skjermede person record
        } else {
            // Create new record with tilknyttet_ressurs = null
            skjermetPerson.setOpprettetDato(LocalDateTime.now());
        }
        repository.save(skjermetPerson);
    }
}
