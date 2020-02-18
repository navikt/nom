package no.nav.nom.skjermede.personer.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class SkjermedePersonConsumer {
    // Create REST calls to create, update & check if exists
    private SkjermetPersonRepository repository;

    public void processRecord(ConsumerRecord<String, JsonNode> record){

        JsonNode jsonNode = record.value();


        String personIdent = "00";
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
