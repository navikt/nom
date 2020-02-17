package no.nav.nom.skjermede.personer.consumer;

import lombok.AllArgsConstructor;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class KafkaSkjermedePersonListener {

    private SkjermetPersonRepository repository;

    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(String record){

        System.out.println("Received message : " + record);
        String personIdent = record;
        if(repository.existsById(personIdent)){
            // Update Skjermede person record just the endret_dato
            updateSkjermetRecord(personIdent);
        }else{
            // Create new record with tilknyttet_ressurs = null
            createSkjermetRecord(personIdent);
        }
    }

    private void updateSkjermetRecord(String personIdent) {
        SkjermetPerson skjermetPerson = repository.getOne(personIdent);
        skjermetPerson.setEndretDato(LocalDateTime.now());
    }

    private void createSkjermetRecord(String personIdent){

    }
}
