package no.nav.nom.libs.state.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.springframework.context.event.EventListener;
import org.springframework.core.log.LogAccessor;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.SerializationUtils;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractNomStateListener<T> {
    protected final String stateTopic;
    protected final String listenerId;

    private AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private AtomicBoolean abortedStateConsumption = new AtomicBoolean(false);
    private boolean started = false;
    private final List<Visitor<T>> visitors = new CopyOnWriteArrayList<>();
    private final Map<String, CompletableFuture<T>> waitingMapByKey = Collections.synchronizedMap(new HashMap<>());
    private final AtomicInteger consumedMessages = new AtomicInteger(0);

    private final String reflectedClassTypeName = ((Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getSimpleName();

    public AbstractNomStateListener(String stateTopic, String listenerId) {
        this.stateTopic = stateTopic;
        this.listenerId = listenerId;
    }

    protected abstract void registerMessageWithState(String key, T stateObject);

    protected abstract void doneReadToEndOnStartup();

    protected void onMessageWithHeaders(List<T> events, List<String> keys, List<Map<String, Object>> headers) {
        for (int idx = 0; idx < events.size(); idx++) {
            T event = events.get(idx);
            String key = keys.get(idx);
            var header = headers.get(idx);
            var deserException = getDeserializationException(header);
            if (event == null && deserException != null) {
                String msg = "Conversion/deserialization error in BatchListener %s for event key: %s".formatted(listenerId, key);
                handleMessageListerError(msg, deserException, idx);
            }
            try {
                processMessage(key, event);
            } catch (RuntimeException e) {
                String msg = "Error in processing in BatchListener %s for event key: %s".formatted(listenerId, key);
                handleMessageListerError(msg, e, idx);
            }
        }
    }

    private DeserializationException getDeserializationException(Map<String, Object> header) {
        return header.get(SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER) != null ?
            SerializationUtils.byteArrayToDeserializationException(
                new LogAccessor(this.getClass()),
                (Header)header.get(SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER))
            : null;
    }

    private void handleMessageListerError(String msg, Exception e, int idx) {
        if (!hasReadToEndOnStartup()) {
            // Unrecoverable - do not end up setting hasReadToEndOnStartup to true
            setAbortedOnStartup();
            log.error("ALERTING STATE LISTENER ERROR - on startup for listener {}:  {}",listenerId, msg, e); // NB! Alert in kibana for the text 'ALERTING STATE LISTENER ERROR'
        } else {
            log.error("ALERTING STATE LISTENER ERROR - during normal run for listener {}:  {}",listenerId, msg, e);
        }
        // Fail gracefully - e.g. keep processing: Container DefaultErrorHandler(new FixedBackOff(0L, 0L))); // No retry on error - just log and continue
        throw new BatchListenerFailedException(msg, e, idx);
    }

    private void processMessage(String key, T value) {
        if (!started) {
            log.info("Kafka listener starting - [{}]. {} visitors registered: {}", listenerId, visitors.size(), visitorsToString());
            started = true;
        }

        registerMessageWithState(key, value);

        notifyVisitors(key, value);

        if (waitingMapByKey.containsKey(key)) {
            waitingMapByKey.remove(key).complete(value);
            log.debug("Done waiting for event key in listener: {}", key);
        }

        consumedMessages.incrementAndGet();
    }

    public interface Visitor<T> {
        String visitorName();

        void visit(String key, T stateObject);
    }

    public void register(Visitor<T> visitor) {
        log.info("Registered visitor for listener {} with name: {}", listenerId, visitor.visitorName());
        visitors.add(visitor);
    }

    public void unregister(Visitor<T> visitor) {
        log.info("Unregistering visitor for listener {} with name: {}", listenerId, visitor.visitorName());
        visitors.remove(visitor);
    }

    protected String visitorsToString() {
        return visitors.stream().map(Visitor::visitorName).reduce((a, b) -> a + ", " + b).orElse("none");
    }

    private void notifyVisitors(String key, T stateObject) {
        var visitorErrors = new HashMap<Visitor<T>, VisitorError<T>>();
        visitors.forEach(v -> {
            try {
                v.visit(key, stateObject);
            } catch (Exception e) {
                log.warn("Error in visitor {} for key {}", v.visitorName(), key, e);
                visitorErrors.put(v, new VisitorError<>(key, stateObject, e));
                // Keep processing other visitors
            }
        });
        if (!visitorErrors.isEmpty()) {
            throw new StateVisitorException(
                "Error in visitors %s in Listener %s, for key %s".formatted( 
                    visitorErrors.keySet().stream().map(Visitor::visitorName).toList(), listenerId, key
                )
            );
        }
    }

    public CompletableFuture<T> waitForStateAfterEventCreation(String key) {
        log.debug("Waiting for FID in {}}: {}", listenerId, key);
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        waitingMapByKey.put(key, completableFuture);
        return completableFuture;
    }

    public void removeWait(String fid) {
        waitingMapByKey.remove(fid);
    }

    public boolean hasReadToEndOnStartup() {
        return hasReadToEndOnStartup.get();
    }

    public boolean isAbortedOnStartup() {
        return abortedStateConsumption.get();
    }

    public void resetStateConsumption() {
        hasReadToEndOnStartup.set(false);
        abortedStateConsumption.set(false);
    }

    public void setHasReadToEndOnStartup() {
        hasReadToEndOnStartup.set(true);
    }

    public synchronized void setAbortedOnStartup() {
        abortedStateConsumption.set(true);
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup() || isAbortedOnStartup()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            if (isAbortedOnStartup()) {
                log.warn("Idle event received for listener {} - but state consumption has been aborted", listenerId);
                return;
            }

            log.info(
                "All Events from topic [{}] has now been read on startup by listener [{}]. Number of {} messages consumed: {}", stateTopic, listenerId, reflectedClassTypeName, consumedMessages
            );
            setHasReadToEndOnStartup();
            doneReadToEndOnStartup();
        }
    }
}
