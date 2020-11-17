package no.nav.nom.skjermede.personer.pip;

import no.nav.nom.contract.skjermede.dto.SkjermetPersonDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@EnableKafka
public class KafkaTestProducerService {

    private final KafkaTemplate<String, SkjermetPersonDTO> kafkaTemplate;

    @Value("${app.kafka.topic.skjermet.name}")
    private String TOPIC_NAME;

    @Autowired
    public KafkaTestProducerService(KafkaTemplate<String, SkjermetPersonDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String personId, SkjermetPersonDTO skjermetPersonDTO) {
        kafkaTemplate.send(TOPIC_NAME, personId, skjermetPersonDTO);
    }

}
