package no.nav.nom.libs.state.authorize;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.LederTilgangState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class DelegertLederStateRepositoryImpl implements DelegertLederStateRepository {
    private final Map<String, LederTilgangState> delegertLederByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<LederTilgangState>>> delegertLederByNavidentAndEnhet = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<LederTilgangState>>> delegertLederByEnhetAndNavident = new ConcurrentHashMap<>();

    @Override
    public Optional<LederTilgangState> findByFid(String fid) {
        return Optional.ofNullable(delegertLederByFid.get(fid));
    }

    @Override
    public List<LederTilgangState> findByNavident(String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream().flatMap(List::stream).toList();
    }

    @Override
    public List<LederTilgangState> findDelegertAvNavident(String navident) {
        return delegertLederByFid.values().stream().filter(o -> o.av().equals(navident)).toList();
    }

    @Override
    public List<String> aktiveDelegeringerForOrgenheterTilNavident(String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream()
            .flatMap(List::stream)
            .filter(LederTilgangState::isActive)
            .map(LederTilgangState::orgenhetId)
            .toList();
    }

    @Override
    public boolean erOrgenhetDelegertTilNavident(String orgenhetId, String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).containsKey(orgenhetId);
    }

    @Override
    public Optional<LederTilgangState> lederTilgangForOrgenhetOgNavident(String orgenhetId, String navident) {
        return delegertLederByNavidentAndEnhet.getOrDefault(navident, Collections.emptyMap()).values().stream().flatMap(List::stream)
            .filter(lederTilgang -> lederTilgang.orgenhetId().equals(orgenhetId))
            .filter(LederTilgangState::isActive)
            .findFirst();
    }

    @Override
    public Collection<LederTilgangState> getAll() {
        return delegertLederByFid.values();
    }

    @Override
    public int count() {
        return delegertLederByFid.size();
    }

    @Override
    public void update(LederTilgangState state) {
        var oldState = delegertLederByFid.get(state.fid());
        delegertLederByFid.put(state.fid(), state);

        if (oldState!= null) {
            removeFromByNavidentAndEnhet(oldState);
            removeFromByEnhetAndNavident(oldState);
        }

        addToByNavidentAndEnhet(state);
        addToByEnhetAndNavident(state);
    }

    @Override
    public void clear() {
        delegertLederByFid.clear();
        delegertLederByNavidentAndEnhet.clear();
        delegertLederByEnhetAndNavident.clear();
    }

    private void addToByNavidentAndEnhet(LederTilgangState newState) {
        delegertLederByNavidentAndEnhet.computeIfAbsent(newState.til(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.orgenhetId(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void addToByEnhetAndNavident(LederTilgangState newState) {
        delegertLederByEnhetAndNavident.computeIfAbsent(newState.orgenhetId(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(newState.til(), k -> new CopyOnWriteArrayList<>())
            .add(newState);
    }

    private void removeFromByNavidentAndEnhet(LederTilgangState oldState) {
        delegertLederByNavidentAndEnhet.getOrDefault(oldState.til(), Collections.emptyMap()).get(oldState.orgenhetId()).remove(oldState);
    }

    private void removeFromByEnhetAndNavident(LederTilgangState oldState) {
        delegertLederByEnhetAndNavident.getOrDefault(oldState.orgenhetId(), Collections.emptyMap()).get(oldState.til()).remove(oldState);
    }
}
