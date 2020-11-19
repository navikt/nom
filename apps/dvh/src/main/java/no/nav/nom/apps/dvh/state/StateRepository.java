package no.nav.nom.apps.dvh.state;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class StateRepository {
    private final Map<String, Long> currentState = new ConcurrentHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final StateLoader stateLoader;

    public Optional<Long> findSequenceFor(String key) {
        if (!loaded.get()) {
            synchronized (loaded) {
                if (!loaded.get()) {
                    val newState = stateLoader.loadState();
                    currentState.putAll(newState);
                    loaded.set(true);
                }
            }
        }

        return Optional.ofNullable(currentState.get(key));
    }

    public void saveOffset(String key, long offset) {
        currentState.put(key, offset);
    }
}
