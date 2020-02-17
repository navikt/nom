package no.nav.nom.apps.nom.ressurser.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KafkaAdminConsumer {

    //private final String kafkaRestUrl;

    //private final RestTemplate restTemplate;

    public void lagNyTopic(String topicName, int partisjoner){

    }

    public void hentTopicConfig(String topicName){

    }

}
