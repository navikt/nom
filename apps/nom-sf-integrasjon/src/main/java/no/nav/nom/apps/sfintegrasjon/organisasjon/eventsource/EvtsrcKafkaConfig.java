package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableConfigurationProperties(EvtsrcKafkaProperties.class)
public class EvtsrcKafkaConfig {

    /**
     * Dedikert ConsumerFactory for eventsource-topics med statisk group.id og auto.offset.reset=latest.
     * Statisk group.id sikrer at committed offsets overlever restart, slik at vi ikke re-leser meldinger.
     */
    @Bean
    public ConsumerFactory<String, String> evtsrcConsumerFactory(
        KafkaProperties kafkaProperties,
        EvtsrcKafkaProperties evtsrcProperties
    ) {
        var props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, evtsrcProperties.getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> organisasjonEvtsrcKafkaListenerContainerFactory(
        ConsumerFactory<String, String> evtsrcConsumerFactory,
        KafkaListenerEndpointRegistry registry
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(evtsrcConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(new CommonStopAlleContainereErrorHandler(registry));
        return factory;
    }

    /**
     * AdminClient brukes av EvtsrcKafkaListener for å sjekke committed offsets ved oppstart.
     */
    @Bean
    public AdminClient evtsrcAdminClient(KafkaProperties kafkaProperties) {
        return AdminClient.create(kafkaProperties.buildAdminProperties());
    }
}
