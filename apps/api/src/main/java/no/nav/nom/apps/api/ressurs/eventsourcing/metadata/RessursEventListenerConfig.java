package no.nav.nom.apps.api.ressurs.eventsourcing.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Random;

@Configuration
public class RessursEventListenerConfig {


    @Bean
    ConsumerFactory<String, RessursEvent> ressursEventConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonDeserializer = new JsonDeserializer<>(RessursEvent.class, objectMapper);

        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, RessursEvent> ressursEventListenerContainerFactory(ConsumerFactory<String, RessursEvent> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, RessursEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);

        factory.setContainerCustomizer(container ->
            container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10))
        );

        return factory;
    }

    @Bean
    public String ressursEventUniqueListenerId() { return getUniqueListenerIdString(); }

    /*
     * POD Hostname er ikke unikt nok alene.
     * POD oppstart kan ha startet å kunsumere meldinger.
     * Hvis POD'en restarter (pga f.eks. oppstartsfeil) så vil den ikke få ny hostname, og tidligere konsumertemeldinger vil ikke leses på nytt i ny instans.
     */
    protected String getUniqueListenerIdString() {
        int rnd = Math.abs(new Random().nextInt());
        while (rnd < 0) {  rnd = Math.abs(new Random().nextInt()); }
        return "ressursEvtSrcListener-" + rnd;
    }
}
