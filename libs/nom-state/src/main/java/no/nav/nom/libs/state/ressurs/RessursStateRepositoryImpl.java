package no.nav.nom.libs.state.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.libs.utils.common.util.DateUtils.sluttdatoIsAfterToday;
import static no.nav.nom.libs.utils.common.util.DateUtils.startdatoIsAfterToday;

@Slf4j
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
    public Map<String, RessursState> findByPersonidentBulk(List<String> personIdenter) {
        return ressursByPersonident.entrySet().stream()
            .filter(entry -> personIdenter.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, RessursState> findByNavidentBulk(List<String> navIdenter) {
        Set<String> navidenter = new HashSet<>(navIdenter);
        return ressursByNavident.entrySet().stream()
            .filter(entry -> navidenter.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, RessursState> findByFidBulk(List<String> alleFid) {
        Set<String> fider = new HashSet<>(alleFid);
        return ressursByFid.entrySet().stream()
            .filter(entry -> fider.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, RessursState> findAllWithSluttdatoBeforeToday() {
        return ressursByNavident.values().stream()
            .filter(ressurs -> ressurs.sluttdato() != null)
            .filter(ressurs -> ressurs.sluttdato().isBefore(LocalDate.now()))
            .collect(Collectors.toMap(RessursState::navident, ressurs -> ressurs));
    }

    @Override
    public Map<String, RessursState> findAllActiveRessurser() {
        return ressursByNavident.values().stream()
            .filter(ressurs -> isNull(ressurs.sluttdato()) || !ressurs.sluttdato().isBefore(LocalDate.now()))
            .collect(Collectors.toMap(RessursState::navident, ressurs -> ressurs));
    }

    @Override
    public void update(RessursState ressursEventData) {
        ressursByFid.put(ressursEventData.fid(), ressursEventData);
        ressursByPersonident.put(ressursEventData.personident(), ressursEventData);
        ressursByNavident.put(ressursEventData.navident(), ressursEventData);
        if (ressursEventData.sektor() != null && ressursEventData.sektor() == Sektor.EKSTERN) {
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

    // -- METRICS --

    @Override
    public long count() {
        return ressursByFid.size();
    }

    @Override
    public long countEksterne() {
        return getAll().stream().filter(RessursState::isEkstern).count();
    }

    @Override
    public long countKommunale() {
        return getAll().stream().filter(RessursState::isKommunal).count();
    }

    @Override
    public long countStatlige() {
        return getAll().stream().filter(RessursState::isStatlig).count();
    }

    @Override
    public long countIkkeAvsluttede() {
        return getAll().stream().filter( r -> sluttdatoIsAfterToday(r.sluttdato())).count();
    }

    @Override
    public long countIkkeAvsluttedeEksterne() {
        return getAll().stream().filter(RessursState::isEkstern).filter( r -> sluttdatoIsAfterToday(r.sluttdato())).count();
    }

    @Override
    public long countIkkeAvsluttedeKommunale() {
        return getAll().stream().filter(RessursState::isKommunal).filter( r -> sluttdatoIsAfterToday(r.sluttdato())).count();
    }

    @Override
    public long countIkkeAvsluttedeStatlige() {
        return getAll().stream().filter(RessursState::isStatlig).filter( r -> sluttdatoIsAfterToday(r.sluttdato())).count();
    }

    @Override
    public long countFremtidige() {
        return getAll().stream().filter(r -> startdatoIsAfterToday(r.startdato())).count();
    }

    @Override
    public long countFremtidigeEksterne() {
        return getAll().stream().filter(RessursState::isEkstern).filter(r -> startdatoIsAfterToday(r.startdato())).count();
    }

    @Override
    public long countFremtidigeKommunale() {
        return getAll().stream().filter(RessursState::isKommunal).filter(r -> startdatoIsAfterToday(r.startdato())).count();
    }

    @Override
    public long countFremtidigeStatlige() {
        return getAll().stream().filter(RessursState::isStatlig).filter(r -> startdatoIsAfterToday(r.startdato())).count();
    }


}
