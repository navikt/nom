package no.nav.nom.apps.api.authorize.eventsourcing.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
// Kan ha flere like delegeringer for samme navident og enhet, men med forskjellige datoperioder
public class DelegertLederStateRepository {
    private final Map<String, DelegertLeder> delegertLederByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<DelegertLeder>>> delegertLederByNavidentAndEnhet = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<DelegertLeder>>> delegertLederByEnhetAndNavident = new ConcurrentHashMap<>();

    public Optional<DelegertLeder> findByFid(String fid) {
        return Optional.ofNullable(delegertLederByFid.get(fid));
    }

    public Collection<DelegertLeder> findByNavident(String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream().flatMap(List::stream).toList();
    }

    public Collection<DelegertLeder> getAll() {
        return delegertLederByFid.values();
    }

    public int count() {
        return delegertLederByFid.size();
    }

    public void createDelegering(DelegertLeder newState) {
        delegertLederByFid.put(newState.fid(), newState);
        addToByNavidentAndEnhet(newState);
        addToByEnhetAndNavident(newState);
    }

    public void updateDelegering(DelegertLeder newState) {
        var oldState = delegertLederByFid.get(newState.fid());
        delegertLederByFid.put(newState.fid(), newState);

        removeFromByNavidentAndEnhet(oldState);
        addToByNavidentAndEnhet(newState);

        removeFromByEnhetAndNavident(oldState);
        addToByEnhetAndNavident(newState);
    }

    public void clear() {
        delegertLederByFid.clear();
        delegertLederByNavidentAndEnhet.clear();
        delegertLederByEnhetAndNavident.clear();
    }

    private void addToByNavidentAndEnhet(DelegertLeder newState) {
        delegertLederByNavidentAndEnhet.computeIfAbsent(newState.til(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.orgenhetId(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void addToByEnhetAndNavident(DelegertLeder newState) {
        delegertLederByEnhetAndNavident.computeIfAbsent(newState.orgenhetId(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.til(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void removeFromByNavidentAndEnhet(DelegertLeder oldState) {
        delegertLederByNavidentAndEnhet.getOrDefault(oldState.til(), Collections.emptyMap()).get(oldState.orgenhetId()).remove(oldState);
    }

    private void removeFromByEnhetAndNavident(DelegertLeder oldState) {
        delegertLederByEnhetAndNavident.getOrDefault(oldState.orgenhetId(), Collections.emptyMap()).get(oldState.til()).remove(oldState);
    }

}
