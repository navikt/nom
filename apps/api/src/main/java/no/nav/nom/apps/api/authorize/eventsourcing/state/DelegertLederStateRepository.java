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
    private final Map<String, LederTilgang> delegertLederByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<LederTilgang>>> delegertLederByNavidentAndEnhet = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<LederTilgang>>> delegertLederByEnhetAndNavident = new ConcurrentHashMap<>();

    public Optional<LederTilgang> findByFid(String fid) {
        return Optional.ofNullable(delegertLederByFid.get(fid));
    }

    public List<LederTilgang> findByNavident(String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream().flatMap(List::stream).toList();
    }

    public List<LederTilgang> findDelegertAvNavident(String navident) {
        return delegertLederByFid.values().stream().filter(o -> o.av().equals(navident)).toList();
    }

    public List<String> aktiveDelegeringerForOrgenheterTilNavident(String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream()
            .flatMap(List::stream)
            .filter(LederTilgang::isActive)
            .map(LederTilgang::orgenhetId)
            .toList();
    }

    public boolean erOrgenhetDelegertTilNavident(String orgenhetId, String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).containsKey(orgenhetId);
    }

    public Optional<LederTilgang> lederTilgangForOrgenhetOgNavident(String orgenhetId, String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream().flatMap(List::stream)
            .filter(lederTilgang -> lederTilgang.orgenhetId().equals(orgenhetId))
            .filter(LederTilgang::isActive)
            .findFirst();
    }

    public Collection<LederTilgang> getAll() {
        return delegertLederByFid.values();
    }

    public int count() {
        return delegertLederByFid.size();
    }

    public void createDelegering(LederTilgang newState) {
        delegertLederByFid.put(newState.fid(), newState);
        addToByNavidentAndEnhet(newState);
        addToByEnhetAndNavident(newState);
    }

    public void updateDelegering(LederTilgang newState) {
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

    private void addToByNavidentAndEnhet(LederTilgang newState) {
        delegertLederByNavidentAndEnhet.computeIfAbsent(newState.til(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.orgenhetId(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void addToByEnhetAndNavident(LederTilgang newState) {
        delegertLederByEnhetAndNavident.computeIfAbsent(newState.orgenhetId(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.til(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void removeFromByNavidentAndEnhet(LederTilgang oldState) {
        delegertLederByNavidentAndEnhet.getOrDefault(oldState.til(), Collections.emptyMap()).get(oldState.orgenhetId()).remove(oldState);
    }

    private void removeFromByEnhetAndNavident(LederTilgang oldState) {
        delegertLederByEnhetAndNavident.getOrDefault(oldState.orgenhetId(), Collections.emptyMap()).get(oldState.til()).remove(oldState);
    }

}
