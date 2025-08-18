package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonidentOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPrimaryTelefonnummerOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursStartDatoOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursTekniskSlettetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Properties;

@EnableKafka
@Configuration
@EnableKafkaStreams
class KafkaConfig {
    @Bean
    public JsonSerde<Object> objectSerde(ObjectMapper objectMapper){
        final var objectSerde = new JsonSerde<>(Object.class, objectMapper);
        objectMapper.registerSubtypes(
            RessursFysiskeBrukerstederOppdatertEventV1.class,
            RessursOpprettetEventV1.class,
            RessursPersonaliaOppdatertV1.class,
            RessursPersonidentOppdatertV1.class,
            RessursPrimaryTelefonnummerOppdatertEventV1.class,
            RessursSektorOppdatertV1.class,
            RessursSluttdatoOppdatertEventV1.class,
            RessursStartDatoOppdatertV1.class,
            RessursTekniskSlettetEventV1.class
        );
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursEventExt.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(com.fasterxml.jackson.databind.node.ObjectNode.class.getPackageName());
        return objectSerde.noTypeInfo().ignoreTypeHeaders();
    }

    @Bean
    KafkaStreamsCustomizer kafkaStreamsCustomizer() {
        return custom -> custom.setUncaughtExceptionHandler(exception ->
                StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        );
    }
}
