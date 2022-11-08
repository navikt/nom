package no.nav.nom.apps.kafka.aggregate.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.Event;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;

@EnableKafka
@Configuration
@EnableKafkaStreams
class KafkaConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfiguration(JsonSerde<Object> objectSerde, KafkaProperties kafkaProperties){
        final var props = new HashMap<String, Object>();
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, objectSerde.getClass().getName());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 0);

        props.putAll(kafkaProperties.buildStreamsProperties());

        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public JsonSerde<Object> objectSerde(ObjectMapper objectMapper){
        final var objectSerde = new JsonSerde<>(Object.class, objectMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        return objectSerde;
    }

}
