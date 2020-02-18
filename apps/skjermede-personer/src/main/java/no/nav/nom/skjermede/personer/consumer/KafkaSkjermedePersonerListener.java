package no.nav.nom.skjermede.personer.consumer;

import lombok.AllArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class KafkaSkjermedePersonerListener {

    private SkjermedePersonerConsumer consumer;

    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(ConsumerRecord<String, RessursDto> record) {

        System.out.println("Received message : " + record.value().getPersonident());
        consumer.processRecord(record);
    }
}
