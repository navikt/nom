package no.nav.nom.apps.api.tilgangsenhet;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TilgangsenhetRepository {

    private final NorgService norgService;

    private final Map<String, Tilgangsenhet> cache = new ConcurrentHashMap<>();
    private LocalDateTime lastUpdated;

    public TilgangsenhetRepository(NorgService norgService) {
        this.norgService = norgService;
    }

    public Tilgangsenhet getById(String id) {
        refreshIfStale();
        return cache.get(id);
    }

    public List<Tilgangsenhet> getAll() {
        refreshIfStale();
        return cache.values().stream().toList();
    }

    public boolean isValid(String tilgangsenhetId) {
        return getById(tilgangsenhetId) != null || cache.isEmpty();
    }

    public void refreshIfStale() {
        if (lastUpdated == null || Duration.between(lastUpdated, LocalDateTime.now()).toMinutes() > 60) {
            var updatedCache = norgService.fetch().stream()
                .map(Tilgangsenhet::fromNorgEnhet)
                .collect(Collectors.toMap(Tilgangsenhet::enhetId, Function.identity()));

            if (!updatedCache.isEmpty()) {
                cache.clear();
                cache.putAll(updatedCache);
                lastUpdated = LocalDateTime.now();
            }
        }
    }
}
