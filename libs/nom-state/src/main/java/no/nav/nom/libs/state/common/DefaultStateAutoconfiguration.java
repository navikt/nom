package no.nav.nom.libs.state.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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

    protected String getListenerIdString(String listenerId) {
        if (isLocal()) {
            return listenerId + "-" + Math.abs(new Random().nextInt());
        }
        return listenerId;
    }

    protected boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }
}
