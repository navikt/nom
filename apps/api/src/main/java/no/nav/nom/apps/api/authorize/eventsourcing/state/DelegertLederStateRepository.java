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
    private final Map<String, DelegertLederRolle> delegertLederByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<DelegertLederRolle>>> delegertLederByNavidentAndEnhet = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<DelegertLederRolle>>> delegertLederByEnhetAndNavident = new ConcurrentHashMap<>();

    public Optional<DelegertLederRolle> findByFid(String fid) {
        return Optional.ofNullable(delegertLederByFid.get(fid));
    }

    public List<DelegertLederRolle> findByNavident(String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream().flatMap(List::stream).toList();
    }

    public List<String> aktiveDelegeringerForOrgenheterTilNavident(String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream()
            .flatMap(List::stream)
            .filter(DelegertLederRolle::isActive)
            .map(DelegertLederRolle::orgenhetId)
            .toList();
    }

    public boolean erOrgenhetDelegertTilNavident(String orgenhetId, String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).containsKey(orgenhetId);
    }

    public Optional<DelegertLederRolle> delegertLederRolleForOrgenhetOgNavident(String orgenhetId, String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream().flatMap(List::stream)
            .filter(delegertLederRolle -> delegertLederRolle.orgenhetId().equals(orgenhetId))
            .filter(DelegertLederRolle::isActive)
            .findFirst();
    }

    public Collection<DelegertLederRolle> getAll() {
        return delegertLederByFid.values();
    }

    public int count() {
        return delegertLederByFid.size();
    }

    public void createDelegering(DelegertLederRolle newState) {
        delegertLederByFid.put(newState.fid(), newState);
        addToByNavidentAndEnhet(newState);
        addToByEnhetAndNavident(newState);
    }

    public void updateDelegering(DelegertLederRolle newState) {
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

    private void addToByNavidentAndEnhet(DelegertLederRolle newState) {
        delegertLederByNavidentAndEnhet.computeIfAbsent(newState.til(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.orgenhetId(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void addToByEnhetAndNavident(DelegertLederRolle newState) {
        delegertLederByEnhetAndNavident.computeIfAbsent(newState.orgenhetId(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.til(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void removeFromByNavidentAndEnhet(DelegertLederRolle oldState) {
        delegertLederByNavidentAndEnhet.getOrDefault(oldState.til(), Collections.emptyMap()).get(oldState.orgenhetId()).remove(oldState);
    }

    private void removeFromByEnhetAndNavident(DelegertLederRolle oldState) {
        delegertLederByEnhetAndNavident.getOrDefault(oldState.orgenhetId(), Collections.emptyMap()).get(oldState.til()).remove(oldState);
    }

}
