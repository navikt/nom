package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EvtsrcStoppingErrorHandlerTest {
    private final KafkaListenerEndpointRegistry registry = mock(KafkaListenerEndpointRegistry.class);
    private final MessageListenerContainer orgEnhet = container(EvtsrcKafkaListener.ORGENHET_LISTENER_ID);
    private final MessageListenerContainer leder = container(EvtsrcKafkaListener.ORGENHET_LEDER_LISTENER_ID);
    private final MessageListenerContainer organisering = container(EvtsrcKafkaListener.ORGANISERING_LISTENER_ID);
    private final Consumer<?, ?> consumer = mock(Consumer.class);

    private MessageListenerContainer container(String id) {
        var container = mock(MessageListenerContainer.class);
        when(container.getListenerId()).thenReturn(id);
        var running = new AtomicBoolean(true);
        when(container.isRunning()).thenAnswer(_ -> running.get());
        doAnswer(invocation -> {
            running.set(false);
            return null;
        }).when(container).stopAbnormally(any(Runnable.class));
        when(registry.getListenerContainer(id)).thenReturn(container);
        return container;
    }

    @Test
    void stopsAllThreeContainersAndPropagatesFailedRecord() {
        var handler = new CommonStopAlleContainereErrorHandler(Runnable::run, registry);
        var failure = new IllegalArgumentException("Feil ved behandling");
        var failed = new ConsumerRecord<>("org.nom.orgenhet-eventsource", 0, 42L, "key", "melding");

        assertThat(handler.seeksAfterHandling()).isTrue();
        assertThatThrownBy(() -> handler.handleRemaining(failure, List.of(failed), consumer, orgEnhet))
            .isInstanceOf(KafkaException.class)
            .hasCause(failure);

        verify(orgEnhet).stopAbnormally(any(Runnable.class));
        verify(leder).stopAbnormally(any(Runnable.class));
        verify(organisering).stopAbnormally(any(Runnable.class));
        verifyNoInteractions(consumer);
    }

    @Test
    void stopsAllThreeOnFailureWithoutRecord() {
        var handler = new CommonStopAlleContainereErrorHandler(Runnable::run, registry);
        var failure = new IllegalStateException("poll feilet");

        assertThatThrownBy(() -> handler.handleOtherException(failure, consumer, leder, false))
            .isInstanceOf(KafkaException.class)
            .hasCause(failure);

        verify(orgEnhet).stopAbnormally(any(Runnable.class));
        verify(leder).stopAbnormally(any(Runnable.class));
        verify(organisering).stopAbnormally(any(Runnable.class));
    }

    @Test
    void stopsAllThreeOnBatchFailure() {
        var handler = new CommonStopAlleContainereErrorHandler(Runnable::run, registry);
        var failed = new ConsumerRecord<>("org.nom.orgenhet-leder-eventsource", 0, 1105716L, "key", "melding");
        var batch = new ConsumerRecords<>(Map.of(new TopicPartition(failed.topic(), failed.partition()), List.of(failed)), Map.of());
        var failure = new BatchListenerFailedException("Feil ved behandling", failed);

        assertThatThrownBy(() -> handler.handleBatch(failure, batch, consumer, leder, () -> {}))
            .isInstanceOf(KafkaException.class)
            .hasCause(failure);

        verify(orgEnhet).stopAbnormally(any(Runnable.class));
        verify(leder).stopAbnormally(any(Runnable.class));
        verify(organisering).stopAbnormally(any(Runnable.class));
        verifyNoInteractions(consumer);
    }

    @Test
    void unexpectedContainerPropagatesFailureWithoutStoppingEvtsrc() {
        var handler = new CommonStopAlleContainereErrorHandler(Runnable::run, registry);
        var other = mock(MessageListenerContainer.class);
        when(other.getListenerId()).thenReturn("annen-listener");
        var failure = new IllegalArgumentException("Feil ved behandling");
        var failed = new ConsumerRecord<>("annet-topic", 0, 42L, "key", "melding");

        assertThatThrownBy(() -> handler.handleRemaining(failure, List.of(failed), consumer, other))
            .isInstanceOf(KafkaException.class)
            .hasCause(failure);

        verify(orgEnhet, never()).stopAbnormally(any(Runnable.class));
        verify(leder, never()).stopAbnormally(any(Runnable.class));
        verify(organisering, never()).stopAbnormally(any(Runnable.class));
        verifyNoInteractions(consumer);
    }
}
