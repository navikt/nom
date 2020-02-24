package no.nav.nom.skjermede.personer.consumer;

import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SkjermedePersonerConsumer {

    private final SkjermetPersonRepository repository;

    @Transactional
    public void processRecord(ConsumerRecord<String, RessursDto> record){

        RessursDto ressursDto = record.value();
        SkjermetPerson skjermetPerson = ConsumerRecordToSkjermetPersonMapper.consumerRecordToSkjermetPersonMapper(ressursDto);

        skjermetPerson.setEndretDato(LocalDateTime.now());

        skjermetPerson = Optional
                .ofNullable(repository.findDistinctByPersonidentAndSkjermetTomNull(skjermetPerson.getPersonident()))
                .orElse(skjermetPerson);

        skjermetPerson.setFunksjonellId(repository.nextFunctionalId());
        skjermetPerson.setOpprettetDato(skjermetPerson.getOpprettetDato() == null
                ? LocalDateTime.now()
                : skjermetPerson.getOpprettetDato());

        repository.save(skjermetPerson);
    }
}
