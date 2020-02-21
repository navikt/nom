package no.nav.nom.skjermede.personer.consumer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class KafkaSkjermedePersonerListener {

    private SkjermedePersonerConsumer consumer;

    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(ConsumerRecord<String, RessursDto> record) {

        log.info("Ny transaksjons hendelse fra aapen-nom-ressurs-v1 for funksjonell ID: {}", record.value().getFunksjonellId());
        consumer.processRecord(record);
    }
}
