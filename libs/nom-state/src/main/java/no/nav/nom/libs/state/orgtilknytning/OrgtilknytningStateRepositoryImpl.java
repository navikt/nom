package no.nav.nom.libs.state.orgtilknytning;

import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrgtilknytningStateRepositoryImpl implements OrgtilknytningStateRepository {
    private final Map<String, OrgtilknytningState> orgtilknytningByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgtilknytningState>> orgtilknytningerByNavident = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgtilknytningState>> orgtilknytningerByOrgenhetId = new ConcurrentHashMap<>();

    @Override
    public Optional<OrgtilknytningState> findByFid(String fid) {
        return Optional.ofNullable(orgtilknytningByFid.get(fid));
    }

    @Override
    public List<OrgtilknytningState> findByFidIn(List<String> fids) {
        return orgtilknytningByFid.entrySet().stream()
            .filter(e -> fids.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<OrgtilknytningState> findByNavident(String navident) {
        return orgtilknytningerByNavident.getOrDefault(navident, Collections.emptyMap()).values();
    }

    @Override
    public List<OrgtilknytningState> findByNavidentIn(List<String> navidents) {
        return orgtilknytningerByNavident.entrySet().stream()
            .filter(e -> navidents.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .flatMap(identTilknytninger -> identTilknytninger.values().stream())
            .collect(Collectors.toList());
    }

    @Override
    public Collection<OrgtilknytningState> getAll() {
        return orgtilknytningByFid.values();
    }

    @Override
    public List<String> getAktiveNavidenterPaaEnhet(String orgEnhetId) {
        return getAktiveNavidenterPaaEnhet(orgEnhetId, DOLIs.ANY);
    }

    @Override
    public List<String> getAktiveNavidenterPaaEnhet(String orgEnhetId, DOLIs dolIs) {
        return getOrgtilknytningerPaaEnhet(orgEnhetId, dolIs).stream()
            .filter(this::isCurrentPeriod)
            .map(OrgtilknytningState::navident)
            .distinct()
            .toList();
    }

    @Override
    public List<String> getAktiveEllerFremtidigeNavidenterPaaEnhet(String orgEnhetId) {
        return getAktiveEllerFremtidigeNavidenterPaaEnhet(orgEnhetId, DOLIs.ANY);
    }

    @Override
    public List<String> getAktiveEllerFremtidigeNavidenterPaaEnhet(String orgEnhetId, DOLIs dolIs) {
        return getOrgtilknytningerPaaEnhet(orgEnhetId, dolIs).stream()
            .filter(this::isCurrentOrFuturePeriod)
            .map(OrgtilknytningState::navident)
            .distinct()
            .toList();
    }

    @Override
    public List<OrgtilknytningState> getOrgtilknytningerPaaEnhet(String orgEnhetId) {
        return getOrgtilknytningerPaaEnhet(orgEnhetId, DOLIs.ANY);
    }

    @Override
    public List<OrgtilknytningState> getOrgtilknytningerPaaEnhet(String orgEnhetId, DOLIs dolIs) {
        var alleOTerForEnhet = orgtilknytningerByOrgenhetId.get(orgEnhetId);
        if (alleOTerForEnhet == null) {
            return Collections.emptyList();
        }
        return alleOTerForEnhet.values().stream()
            .filter(ot -> switch (dolIs) {
                case TRUE -> ot.erDagligOppfolging();
                case FALSE -> !ot.erDagligOppfolging();
                case ANY -> true;
            })
            .collect(Collectors.toList());
    }

    @Override
    public int count() {
        return orgtilknytningByFid.size();
    }

    @Override
    public void update(String fid, OrgtilknytningState newOrgtilknytningState) {
        if (newOrgtilknytningState == null) {
            removeFromState(fid);
        } else {
            addToState(newOrgtilknytningState);
        }
    }

    @Override
    public void clear() {
        orgtilknytningByFid.clear();
        orgtilknytningerByNavident.clear();
        orgtilknytningerByOrgenhetId.clear();
    }

    private void addToState(OrgtilknytningState newOrgtilknytningState) {
        orgtilknytningByFid.put(newOrgtilknytningState.fid(), newOrgtilknytningState);

        var navidentOrgtilknytninger = orgtilknytningerByNavident.get(newOrgtilknytningState.navident());
        if (navidentOrgtilknytninger == null) {
            navidentOrgtilknytninger = new ConcurrentHashMap<>();
            orgtilknytningerByNavident.put(newOrgtilknytningState.navident(), navidentOrgtilknytninger);
        }
        navidentOrgtilknytninger.put(newOrgtilknytningState.fid(), newOrgtilknytningState); // Sett inn ny orgtilknytning, eller bytt ut med ny state evt.

        var orgenhetOrgtilknytninger = orgtilknytningerByOrgenhetId.get(newOrgtilknytningState.orgEnhetId());
        if (orgenhetOrgtilknytninger == null) {
            orgenhetOrgtilknytninger = new ConcurrentHashMap<>();
            orgtilknytningerByOrgenhetId.put(newOrgtilknytningState.orgEnhetId(), orgenhetOrgtilknytninger);
        }
        orgenhetOrgtilknytninger.put(newOrgtilknytningState.fid(), newOrgtilknytningState);
    }

    private void removeFromState(String fid) {
        var eksisterede = orgtilknytningByFid.get(fid);
        if (eksisterede != null) {
            var navidentOrgtilknytninger = orgtilknytningerByNavident.get(eksisterede.navident());
            if (navidentOrgtilknytninger != null) {
                navidentOrgtilknytninger.remove(fid);
            }
            var orgenhetOrgtilknytninger = orgtilknytningerByOrgenhetId.get(eksisterede.orgEnhetId());
            if (orgenhetOrgtilknytninger != null) {
                orgenhetOrgtilknytninger.remove(fid);
            }
        }
        orgtilknytningByFid.remove(fid);
    }

    // TODO: Shamelessly copied from PeriodeUtil - endre når ORG-1696 er gjort
    public boolean isCurrentPeriod(OrgtilknytningState orgtilknytning) {
        return isCurrentOrFuturePeriod(orgtilknytning) && orgtilknytning.gyldigFom().isBefore(tomorrow());
    }

    public boolean isCurrentOrFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigTom().isEmpty() || orgtilknytning.gyldigTom().get().isAfter(yesterday());
    }

    public LocalDate today() { return LocalDate.now(); }

    public LocalDate yesterday() { return today().minusDays(1); }

    public LocalDate tomorrow() { return today().plusDays(1); }

}
