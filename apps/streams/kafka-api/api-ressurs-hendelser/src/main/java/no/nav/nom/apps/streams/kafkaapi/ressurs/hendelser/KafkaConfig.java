package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaConfig {

    @Bean
    public JsonSerde<Object> objectSerde(ObjectMapper objectMapper) {
        final var objectSerde = new JsonSerde<>(Object.class, objectMapper);
        objectMapper.registerSubtypes(
                RessursOpprettetEventV1.class,
                RessursSektorOppdatertV1.class
        );

        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursEvent.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrgtilknytningEvent.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(ObjectNode.class.getPackageName());
        return objectSerde;
    }

    // Prosesser melding på nytt hvis det oppstår en exception
    // https://stackoverflow.com/a/56342785
    @Bean
    KafkaStreamsCustomizer kafkaStreamsCustomizer() {
        return custom -> custom.setUncaughtExceptionHandler(exception ->
                StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        );
    }
}
