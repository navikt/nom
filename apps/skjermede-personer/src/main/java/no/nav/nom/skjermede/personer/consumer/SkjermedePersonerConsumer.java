package no.nav.nom.skjermede.personer.consumer;

import lombok.AllArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class SkjermedePersonerConsumer {

    private SkjermetPersonRepository repository;

    @Transactional
    public void processRecord(ConsumerRecord<String, RessursDto> record){

        RessursDto ressursDto = record.value();
        SkjermetPerson skjermetPerson = ConsumerRecordToSkjermetPersonMapper.consumerRecordToSkjermetPersonMapper(ressursDto);

        skjermetPerson.setEndretDato(LocalDateTime.now());

        ArrayList<String> personIdenter = new ArrayList<String>();
        personIdenter.add(skjermetPerson.getPersonident());

        List<SkjermetPerson> lagretSkjermedePersoner = repository.findAllById(personIdenter);

        if (lagretSkjermedePersoner.isEmpty()) {
            lagSkjermetPerson(skjermetPerson);
        }else{
            SkjermetPerson lagret = lagretSkjermedePersoner.stream().filter(e -> e.getSkjermetTom() == null).findFirst().orElse(null);
            if(lagret != null)
            {
                lagret.setSkjermetFom(skjermetPerson.getSkjermetFom());
                lagret.setSkjermetTom(skjermetPerson.getSkjermetTom());
                lagret.setEndretDato(LocalDateTime.now());
                repository.save(lagret);
            }else{
                // Fjern bruk av denne koden når funksjonell_id problemet er løst
                skjermetPerson.setFunksjonellId(111111L);
                lagSkjermetPerson(skjermetPerson);
            }
        }
    }

    private void lagSkjermetPerson(SkjermetPerson skjermetPerson){
        skjermetPerson.setOpprettetDato(LocalDateTime.now());
        repository.save(skjermetPerson);
    }
}
