package no.nav.nom.apps.nom.unit4.kafka;

import no.nav.nom.apps.nom.unit4.AppProperties;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KafkaRessursDltProducer {

    private String topic;
    private KafkaTemplate<String, RessursEvent> kafkaTemplate;


    public KafkaRessursDltProducer(AppProperties properties,
                                   KafkaTemplate<String, RessursEvent> kafkaTemplate) {
        this.topic = properties.ressurs().topic_dlt();
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(RessursEvent ressursEvent, String uuid) {
        List<Header> headers = new ArrayList<>();
        headers.add(new RecordHeader(KafkaHeaders.RECEIVED_KEY, uuid.getBytes()));
        ProducerRecord<String, RessursEvent> record = new ProducerRecord<>(topic, null, uuid, ressursEvent, headers);

        kafkaTemplate.send(record);
    }
}
