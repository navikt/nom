package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@EnableKafka
@Configuration
@EnableKafkaStreams
class KafkaConfig {
    @Bean
    public JacksonJsonSerde<Object> objectSerde(JsonMapper jsonMapper) {
        final var objectSerde = new JacksonJsonSerde<>(Object.class, jsonMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursEvent.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursEventExt.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(ObjectNode.class.getPackageName());
        return objectSerde.noTypeInfo().ignoreTypeHeaders();
    }

    @Bean
    KafkaStreamsCustomizer kafkaStreamsCustomizer() {
        return custom -> custom.setUncaughtExceptionHandler(exception ->
                StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        );
    }
}
