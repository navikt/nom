package no.nav.nom.apps.api.ressurs.eventsourcing.state;

import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    public void remove(String fid) {
        RessursState ressursStateToDelete = ressursByFid.get(fid);

        if (ressursStateToDelete == null) return;

        ressursByFid.remove(fid);

        // Slett ressurs med denne personidenten fra ressursByPersonident, hvis fid er lik
        // Sjekk også om det evt. finnes en annen ressursFid med samme personident - isåfall må byPersonident bytte til denne ResssursState'en
        RessursState ressursByPersonidentForFid = ressursByPersonident.getOrDefault(ressursStateToDelete.personident(), null);
        if (Objects.equals(fid, ressursByPersonidentForFid == null ? null : ressursByPersonidentForFid.fid())) {
            ressursByPersonident.remove(ressursStateToDelete.personident());
            Optional<RessursState> samePersonidentOtherFid = ressursByFid.values().stream()
                .filter(ressursState -> Objects.equals(ressursState.personident(), ressursStateToDelete.personident()))
                .findFirst();

            samePersonidentOtherFid.ifPresent(ressursState -> ressursByPersonident.put(ressursState.personident(), ressursState));
        }

        // Slett ressurs med denne navidenten fra ressursByNavident, hvis fid er lik
        // Sjekk også om det evt. finnes en annen ressursFid med samme navident - isåfall må byNAvident bytte til denne ResssursState'en
        RessursState ressursByNavidentForFid = ressursByNavident.getOrDefault(ressursStateToDelete.navident(), null);
        if (Objects.equals(fid, ressursByNavidentForFid == null ? null : ressursByNavidentForFid.fid())) {
            ressursByNavident.remove(ressursStateToDelete.navident());
            Optional<RessursState> sameNavidentOtherFid = ressursByFid.values().stream()
                .filter(ressursState -> Objects.equals(ressursState.navident(), ressursStateToDelete.navident()))
                .findFirst();

            sameNavidentOtherFid.ifPresent(ressursState -> ressursByNavident.put(ressursState.navident(), ressursState));
        }
    }
}
