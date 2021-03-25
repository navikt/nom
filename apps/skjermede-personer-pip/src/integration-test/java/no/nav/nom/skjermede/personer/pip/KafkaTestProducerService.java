package no.nav.nom.skjermede.personer.pip;

import no.nav.nom.contract.skjermede.dto.SkjermetPersonDTO;
import no.nav.nom.skjermede.personer.pip.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@EnableKafka
public class KafkaTestProducerService {

    private final KafkaTemplate<String, SkjermetPersonDTO> kafkaTemplate;
    private final AppProperties appProperties;

    @Autowired
    public KafkaTestProducerService(KafkaTemplate<String, SkjermetPersonDTO> kafkaTemplate, AppProperties appProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.appProperties = appProperties;
    }

    public void send(String personId, SkjermetPersonDTO skjermetPersonDTO) {
        kafkaTemplate.send(appProperties.getSkjermedePersoner().getTopicName(), personId, skjermetPersonDTO);
    }

}
