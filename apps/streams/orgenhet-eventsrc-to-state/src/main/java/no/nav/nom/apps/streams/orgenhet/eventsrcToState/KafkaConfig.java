package no.nav.nom.apps.streams.orgenhet.eventsrcToState;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

@EnableKafka
@Configuration
@EnableKafkaStreams
class KafkaConfig {
    @Bean
    public JsonSerde<Object> objectSerde(ObjectMapper objectMapper){
        final var objectSerde = new JsonSerde<>(Object.class, objectMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        return objectSerde;
    }

}
