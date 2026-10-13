package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaExceptionLogLevelAware;
import org.springframework.kafka.listener.ListenerUtils;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.c;
import static org.bouncycastle.asn1.x500.style.RFC4519Style.registeredAddress;


/**
 * Basert på CommonContainerStoppingErrorHandler med unntak av at den stopper alle 3 listenere containere, samt logger ALERT message.
 * Exception som trigger handleren vil kastes videre slik at offset for feilet melding ikke committes.
 */
@Slf4j
public class CommonStopAlleContainereErrorHandler extends KafkaExceptionLogLevelAware implements CommonErrorHandler {
    private final Executor executor;
    private final KafkaListenerEndpointRegistry registry;

    private boolean stopContainerAbnormally = true;

    public CommonStopAlleContainereErrorHandler(KafkaListenerEndpointRegistry registry) {
        this(new SimpleAsyncTaskExecutor("containerStop-"), registry);
    }

    public CommonStopAlleContainereErrorHandler(Executor executor, KafkaListenerEndpointRegistry registry) {
        Assert.notNull(executor, "'executor' cannot be null");
        this.executor = executor;
        this.registry = registry;
    }

    @Override
    public boolean seeksAfterHandling() {
        // We don't actually do any seeks here, but stopping the container has the same effect.
        return true;
    }

    @Override
    public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer,
                                     MessageListenerContainer container, boolean batchListener) {

        stopAllContainers(container, thrownException, "UKJENT");
    }

    @Override
    public void handleRemaining(Exception thrownException, List<ConsumerRecord<?, ?>> records, Consumer<?, ?> consumer,
                                MessageListenerContainer container) {

        stopAllContainers(container, thrownException, String.valueOf(records.getFirst().offset()));
    }

    @Override
    public void handleBatch(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer,
                            MessageListenerContainer container, Runnable invokeListener) {
        String offset = "UKJENT";
        if (thrownException instanceof BatchListenerFailedException bfe) {
            if (bfe.getRecord() != null) {
                offset = String.valueOf(bfe.getRecord().offset());
            }
        }
        stopAllContainers(container, thrownException, offset);
    }

    private void stopAllContainers(MessageListenerContainer container, Exception thrownException, String offset) {
        var containers = List.of(
            Objects.requireNonNull(registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LISTENER_ID)),
            Objects.requireNonNull(registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LEDER_LISTENER_ID)),
            Objects.requireNonNull(registry.getListenerContainer(EvtsrcKafkaListener.ORGANISERING_LISTENER_ID))
        );
        if (!containers.contains(container)) {
            log.warn("Container {} er ikke en av organisasjons lytterne. Vil ikke stoppe disse lytterne.", container.getListenerId());
            throw new KafkaException("CommonStopAlleContainereErrorHandler fikk exception for ukjent container - vil ikke stoppe organisasjons lyttere pga dette.", getLogLevel(), thrownException);
        }
        for (var c : containers) {
            if (c.isRunning()) {
                this.executor.execute(() -> {
                    if (this.stopContainerAbnormally) {
                        c.stopAbnormally(() -> {
                        });
                    } else {
                        c.stop(() -> {
                        });
                    }
                });
            }
        }
        // isRunning is false before the container.stop() waits for listener thread
        try {
            for (var c : containers) {
                ListenerUtils.stoppableSleep(c, 10_000); // NOSONAR
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        var listeners = containers.stream().map(MessageListenerContainer::getListenerId).toList();
        log.error(
            "ALERTING EVTSRC: Feil i behandling av melding i {} listener på offset {}. Har stoppet alle containerene [{}]",
            container.getListenerId(), offset, listeners, thrownException);
        throw new KafkaException("Stopped containers [%s]".formatted(listeners), getLogLevel(), thrownException);
    }
}
