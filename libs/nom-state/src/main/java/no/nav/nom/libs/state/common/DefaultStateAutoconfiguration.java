package no.nav.nom.libs.state.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.HashMap;
import java.util.Random;

@Slf4j
public class DefaultStateAutoconfiguration<T> {
    final Environment environment;
    final int idleEventIntervalSeconds;

    public DefaultStateAutoconfiguration(Environment environment, int idleEventIntervalSeconds) {
        this.environment = environment;
        this.idleEventIntervalSeconds = idleEventIntervalSeconds;
    }

    protected ConcurrentKafkaListenerContainerFactory<String, T> defaultContainerFactorySettings(
        ConsumerFactory<String, T> consumerFactory,
        ConcurrentKafkaListenerContainerFactory<String, T> factory
    ) {
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleEventInterval(idleEventIntervalSeconds * 1000L);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L))); // No retry on error - just log and continue

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container -> {
                container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
                log.info(container.toString());
            }
        );
        return factory;
    }

    final HashMap<String, String> listenerIdMap = new HashMap<>();

    protected String getUniqueListenerIdString(String listenerId) {
        if (!listenerIdMap.containsKey(listenerId)) {
            int rnd = Math.abs(new Random().nextInt());
            while (rnd < 0) {  rnd = Math.abs(new Random().nextInt()); }
            listenerIdMap.put(listenerId, listenerId + "-" + rnd);
        }
        return listenerIdMap.get(listenerId);
    }
}
