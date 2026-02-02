package no.nav.nom.apps.navident.common;

import no.nav.nom.apps.navident.eventsource.SourceEvent;
import no.nav.nom.apps.navident.rnrservices.Need;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Random;

@EnableKafka
@Configuration
public class KafkaConfig {

    /* EventSource Topic */

    @Bean
    public ConsumerFactory<String, SourceEvent> consumerFactory(KafkaProperties properties) {
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, SourceEvent>>
            kafkaListenerContainerFactory(ConsumerFactory<String, SourceEvent> consumerFactory,
                                          @Value("${app.kafka.eventSourceIdleTimeout}") Long idleTimeout) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, SourceEvent>();
        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties().setIdleEventInterval(idleTimeout);

        return factory;
    }

    @Bean
    public ProducerFactory<String, SourceEvent> producerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, SourceEvent> kafkaTemplate(ProducerFactory<String, SourceEvent> producerFactory) {
        KafkaTemplate<String, SourceEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }

    /* Need Topic */

    @Bean
    public ConsumerFactory<String, Need> needConsumerFactory(KafkaProperties properties) {
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Need>> needKafkaListenerContainerFactory(ConsumerFactory<String, Need> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Need>();
        factory.setConsumerFactory(consumerFactory);

        return factory;
    }

    @Bean
    public ProducerFactory<String, Need> needProducerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, Need> needKafkaTemplate(ProducerFactory<String, Need> producerFactory) {
        KafkaTemplate<String, Need> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }

    @Bean
    public String eventsourceUniqueListenerId(@Value("${app.kafka.eventSourceListenerName}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String rnrUniqueListenerId(@Value("${app.kafka.identServiceListenerName}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    final HashMap<String, String> listenerIdMap = new HashMap<>();

    /*
     * POD Hostname er ikke unikt nok alene.
     * POD oppstart kan ha startet å kunsumere meldinger.
     * Hvis POD'en restarter (pga f.eks. oppstartsfeil) så vil den ikke få ny hostname, og tidligere konsumertemeldinger vil ikke leses på nytt i ny instans.
     */
    protected String getUniqueListenerIdString(String listenerId) {
        if (!listenerIdMap.containsKey(listenerId)) {
            int rnd = Math.abs(new Random().nextInt());
            while (rnd < 0) {  rnd = Math.abs(new Random().nextInt()); }
            listenerIdMap.put(listenerId, listenerId + "-" + rnd);
        }
        return listenerIdMap.get(listenerId);
    }
}

