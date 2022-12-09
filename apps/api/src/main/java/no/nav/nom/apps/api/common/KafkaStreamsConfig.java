package no.nav.nom.apps.api.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;

//@Configuration
//@EnableKafkaStreams - This creates a single StreamsBuilderBeanFactory, and do not support multiple streams on same topic. E.g. different Application.id's
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}") private String bootstrap;
    public static final String ORGENHET_STATE_STREAMS_BUILDER_BEAN_NAME = "orgEnhetStateStreamsBuilder";

    @Bean
    public JsonSerde<Object> objectSerde(ObjectMapper objectMapper){
        final var objectSerde = new JsonSerde<>(Object.class, objectMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        return objectSerde;
    }

//    @Bean(name = ORGENHET_STATE_STREAMS_BUILDER_BEAN_NAME)
    public FactoryBean<StreamsBuilder> orgEnhetStateStreamsBuilder(ObjectProvider<StreamsBuilderFactoryBeanCustomizer> customizers) {
        var props = commonStreamsConfigProperties();
        props.put(APPLICATION_ID_CONFIG, "orgEnhet_Eventsource2State");

        var factoryBean = new StreamsBuilderFactoryBean(new KafkaStreamsConfiguration(props));
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factoryBean)); // Add customizer for metrics
        return factoryBean;
    }

    private Map<String, Object> commonStreamsConfigProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
        return props;
    }
}
