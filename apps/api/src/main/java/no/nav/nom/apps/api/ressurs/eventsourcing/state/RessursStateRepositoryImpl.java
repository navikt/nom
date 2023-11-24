package no.nav.nom.apps.api.ressurs.eventsourcing.state;

import no.nav.nom.contract.nom.Sektor;
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
public class RessursStateRepositoryImpl implements RessursStateRepository {
    private final Map<String, RessursState> ressursByFid = new ConcurrentHashMap<>();
    private final Map<String, RessursState> ressursByPersonident = new ConcurrentHashMap<>();
    private final Map<String, RessursState> ressursByNavident = new ConcurrentHashMap<>();
    private final Map<String, RessursState> ressursByNavidentBareEksterne = new ConcurrentHashMap<>();

    @Override
    public Optional<RessursState> findByFid(String fid) {
        return Optional.ofNullable(ressursByFid.get(fid));
    }

    @Override
    public Optional<RessursState> findByPersonident(String personident) {
        return Optional.ofNullable(ressursByPersonident.get(personident));
    }

    @Override
    public Optional<RessursState> findByNavident(String navident) {
        return Optional.ofNullable(ressursByNavident.get(navident));
    }

    @Override
    public <T> Optional<T> findByNavident(String navIdent, Function<RessursState, T> withMapper) {
        return findByNavident(navIdent)
            .map(withMapper);
    }

    @Override
    public Collection<RessursState> getAll() {
        return ressursByNavident.values();
    }

    @Override
    public Collection<RessursState> getAllBareEksterne() {
        return ressursByNavidentBareEksterne.values();
    }

    @Override
    public Collection<RessursState> getAllByFid() {
        return ressursByFid.values();
    }

    @Override
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

    @Override
    public void putEventAggregate(RessursState ressursEventData) {
        ressursByFid.put(ressursEventData.fid(), ressursEventData);
        ressursByPersonident.put(ressursEventData.personident(), ressursEventData);
        ressursByNavident.put(ressursEventData.navident(), ressursEventData);
        if (ressursEventData.sektor().equals(Sektor.EKSTERN)) {
            ressursByNavidentBareEksterne.put(ressursEventData.navident(), ressursEventData);
        }
    }

    @Override
    public void clear() {
        ressursByFid.clear();
        ressursByPersonident.clear();
        ressursByNavident.clear();
        ressursByNavidentBareEksterne.clear();
    }

    @Override
    public void remove(String fid) {
        RessursState ressursStateToDelete = ressursByFid.get(fid);
        if (ressursStateToDelete == null) {
            return;
        }
        ressursByFid.remove(fid);

        removeInRessursByPersonident(fid, ressursStateToDelete);
        removeInRessursByNavident(fid, ressursStateToDelete);
        removeInRessursByNavidentBareEksterne(fid, ressursStateToDelete);
    }

    private void removeInRessursByNavident(String fid, RessursState ressursStateToDelete) {
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

    private void removeInRessursByNavidentBareEksterne(String fid, RessursState ressursStateToDelete) {
        // Slett ressurs med denne navidenten fra ressursByNavidentBareEksterne, hvis fid er lik
        // Sjekk også om det evt. finnes en annen ressursFid med samme navident - isåfall må byNAvident bytte til denne ResssursState'en
        RessursState ressursByNavidentForFid = ressursByNavidentBareEksterne.getOrDefault(ressursStateToDelete.navident(), null);
        if (Objects.equals(fid, ressursByNavidentForFid == null ? null : ressursByNavidentForFid.fid())) {
            ressursByNavidentBareEksterne.remove(ressursStateToDelete.navident());
            Optional<RessursState> sameNavidentOtherFid = ressursByFid.values().stream()
                .filter(ressursState -> Objects.equals(ressursState.navident(), ressursStateToDelete.navident()))
                .findFirst();

            sameNavidentOtherFid.ifPresent(ressursState -> ressursByNavidentBareEksterne.put(ressursState.navident(), ressursState));
        }
    }

    private void removeInRessursByPersonident(String fid, RessursState ressursStateToDelete) {
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
    }
}
