package no.nav.nom.libs.state.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.event.ListenerContainerIdleEvent;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

@Slf4j
public abstract class AbstractNomStateListener<T> {
    protected final String stateTopic;
    protected final String listenerId;

    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
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

    protected void onMessage(ConsumerRecords<String, T> records) {
        StreamSupport.stream(records.spliterator(), false).forEach(it -> {
            processMessage(it.key(), it.value());
        });
    }

    private void processMessage(String key, T value) {
        if (!started) {
            log.info("Kafka listener starting - [{}]. {} visitors registered: {}", listenerId, visitors.size(), visitorsToString());
            started = true;
        }

        registerMessageWithState(key, (T) value);

        if (waitingMapByKey.containsKey(key)) {
            waitingMapByKey.remove(key).complete(value);
            log.debug("Done waiting for event key in listener: {}", key);
        }

        notifyVisitors(key, value);
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

    protected String visitorsToString() {
        return visitors.stream().map(Visitor::visitorName).reduce((a, b) -> a + ", " + b).orElse("none");
    }

    private void notifyVisitors(String key, T stateObject) {
        visitors.forEach(v -> v.visit(key, stateObject));
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

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All SourceEvents has now been read on startup. Number of {} messages consumed: {}", reflectedClassTypeName, consumedMessages
            );
            hasReadToEndOnStartup.set(true);
            doneReadToEndOnStartup();
        }
    }

}
