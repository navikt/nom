package no.nav.nom.apps.admin.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Random;

@Slf4j
@Configuration
@EnableKafka
public class GenericApiKafkaConfig {

    @Bean
    ConsumerFactory<String, Object> genericApiEventDTOConsumerFactory(KafkaProperties properties) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new GenericApiEventDeserializer()
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> genericApiEventKafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory
    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleEventInterval(5 * 1000L);

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container -> {
                container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
                log.info(container.toString());
            }
        );

        return factory;
    }

    @Bean
    public String apiStateHendelserUniqueListenerId(@Value("${app.kafka.hendelser.apiEventTopic-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String apiStateOrgenhetUniqueListenerId(@Value("${app.kafka.orgenhet.apiEventTopic-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String apiStateRessursUniqueListenerId(@Value("${app.kafka.ressurs.apiEventTopic-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String apiStateRessursV3UniqueListenerId(@Value("${app.kafka.ressurs.apiEventTopicV3-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String apiHendelseRessursUniqueListenerId(@Value("${app.kafka.ressurs-hendelser.apiEventTopic-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String evtsrcOrgenhetUniqueListenerId(@Value("${app.kafka.orgenhet.eventsource-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String evtsrcOrgtilknytningUniqueListenerId(@Value("${app.kafka.orgtilknytning.eventsource-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String evtsrcRessursUniqueListenerId(@Value("${app.kafka.ressurs.eventsource-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String evtsrcTilgangUniqueListenerId(@Value("${app.kafka.authorize.eventsource-listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    @Bean
    public String deadLetterUniqueListenerId(@Value("${app.kafka.dead-letter.listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

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
