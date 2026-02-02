package no.nav.nom.apps.streams.kafkaapi.hendelser;

import no.nav.nom.apps.streams.kafkaapi.hendelser.v1.Hendelse;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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
public class KafkaConfig {

    @Bean
    public JacksonJsonSerde<Object> objectSerde(JsonMapper jsonMapper) {
        final var objectSerde = new JacksonJsonSerde<>(Object.class, jsonMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursEvent.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrgEnhetEvent.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrganiseringEvent.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrgtilknytningEvent.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrgEnhetState.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(Hendelse.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(ObjectNode.class.getPackageName());
        return objectSerde;
    }

    @Bean
    KafkaStreamsCustomizer kafkaStreamsCustomizer() {
        return custom -> custom.setUncaughtExceptionHandler(exception ->
            StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        );
    }

}
