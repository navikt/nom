package no.nav.nom.apps.api.organisering.eventsourcing.state;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrganiseringStateRepository {
    private final Map<String, OrganiseringState> organiseringByFid = new ConcurrentHashMap<>();

    public void put(String fid, OrganiseringState organiseringState) {
        organiseringByFid.put(fid, organiseringState);
    }

    public Optional<OrganiseringState> findByFid(String fid) {
        return Optional.ofNullable(organiseringByFid.get(fid));
    }

    public Optional<OrganiseringState> findActiveByFid(String fid) {
        return getActiveOrganisering().containsKey(fid) ? Optional.ofNullable(getActiveOrganisering().get(fid)) : Optional.empty();
    }

    public Optional<OrganiseringState> findFidByAgressoIdNivaAndName(Optional<IOrgenhet> orgEnhet) {
        return orgEnhet.flatMap(iOrgenhet -> organiseringByFid.values()
            .stream()
            .filter(organiseringState -> organiseringState.id().equals(iOrgenhet.getId()))
            .findFirst());
    }

    public void clear() {
        organiseringByFid.clear();
    }

    public void putEventAggregate(OrganiseringState organiseringState) {
        organiseringByFid.put(organiseringState.fid(), organiseringState);
    }

    public int count() {
        return organiseringByFid.size();
    }

    public Map<String, OrganiseringState> getAll() {
        return organiseringByFid;
    }

    public Map<String, OrganiseringState> getActiveOrganisering() {
        // either terminating date is not present of it is today or later
        return organiseringByFid.entrySet().stream()
            .filter(e -> isGyldigTomNull(e.getValue()) || (!isCreatedAndDeletedToday(e.getValue()) && (isGyldigTomTodayOrLater(e.getValue()))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isCreatedAndDeletedToday(OrganiseringState organiseringState) {
        return (organiseringState.gyldigTom().isPresent() &&
            organiseringState.gyldigFom() != null &&
            organiseringState.gyldigTom().get().isEqual(organiseringState.gyldigFom()) &&
            organiseringState.gyldigTom().get().isEqual(LocalDate.now()));
    }

    private boolean isGyldigTomTodayOrLater(OrganiseringState organiseringState) {
        return organiseringState.gyldigTom().isPresent()
            && organiseringState.gyldigTom().get().isAfter(LocalDate.now().minusDays(1));
    }

    private boolean isGyldigTomNull(OrganiseringState organiseringState) {
        LocalDate gyldigTom = Optional.of(organiseringState)
            .filter(state -> state.gyldigTom() != null && state.gyldigTom().isPresent())
            .flatMap(OrganiseringState::gyldigTom)
            .orElse(null);
        return gyldigTom == null;
    }
}
