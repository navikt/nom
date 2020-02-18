package no.nav.nom.skjermede.personer.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class KafkaSkjermedePersonListener {

    private SkjermedePersonConsumer consumer;

    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(ConsumerRecord<String, RessursDto> record) {

        System.out.println("Received message : " + record.value());
        consumer.processRecord(record);
    }
}
