package no.nav.nom.apps.api.ressurs.eventsourcing.state;

import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RessursStateRepository implements IRessursService {
    private final Map<String, RessursState> ressursByFid = new ConcurrentHashMap<>();
    private final Map<String, RessursState> ressursByPersonident = new ConcurrentHashMap<>();
    private final Map<String, RessursState> ressursByNavident = new ConcurrentHashMap<>();

    public Optional<RessursState> findByFid(String fid) {
        return Optional.ofNullable(ressursByFid.get(fid));
    }

    public Optional<RessursState> findByPersonident(String personident) {
        return Optional.ofNullable(ressursByPersonident.get(personident));
    }

    public Optional<RessursState> findByNavident(String navident) {
        return Optional.ofNullable(ressursByNavident.get(navident));
    }

    public <T> Optional<T> findByNavident(String navIdent, Function<RessursState, T> withMapper) {
        return findByNavident(navIdent)
                .map(withMapper);
    }

    public Collection<RessursState> getAll() {
        return ressursByNavident.values();
    }

    public int count() {
        return ressursByFid.size();
    }

    @Override
    public Map<String, RessursState> findByPersonidentBulk(List<String> personIdenter) {
        return ressursByPersonident.entrySet().stream()
                .filter(entry -> personIdenter.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, RessursState> findByNavidentBulk(List<String> navIdenter) {
        return ressursByNavident.entrySet().stream()
                .filter(entry -> navIdenter.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void putEventAggregate(RessursState ressursEventData) {
        ressursByFid.put(ressursEventData.fid(), ressursEventData);
        ressursByPersonident.put(ressursEventData.personident(), ressursEventData);
        ressursByNavident.put(ressursEventData.navident(), ressursEventData);
    }

    @Override
    public void clear() {
        ressursByFid.clear();
        ressursByPersonident.clear();
        ressursByNavident.clear();
    }
}
