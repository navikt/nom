package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.Hendelser;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.support.serializer.JsonSerde;


@EnableKafka
@Configuration
@EnableKafkaStreams
public class KafkaConfig {

    @Bean
    public JsonSerde<Object> objectSerde(ObjectMapper objectMapper) {
        final var objectSerde = new JsonSerde<>(Object.class, objectMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrgEnhetState.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(Hendelser.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(com.fasterxml.jackson.databind.node.ObjectNode.class.getPackageName());
        return objectSerde;
    }

    @Bean
    KafkaStreamsCustomizer kafkaStreamsCustomizer() {
        return custom -> custom.setUncaughtExceptionHandler(exception ->
            StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        );
    }

}
