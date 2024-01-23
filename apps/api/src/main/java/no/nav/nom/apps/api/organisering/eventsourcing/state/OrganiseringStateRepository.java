package no.nav.nom.apps.api.organisering.eventsourcing.state;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrganiseringStateRepository {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, OrganiseringState> organiseringByFid = new ConcurrentHashMap<>(16, 0.9f, 1);
    private final Map<String, OrganiseringState> organiseringById = new ConcurrentHashMap<>(16, 0.9f, 1);
    private final Map<String, Set<OrganiseringState>> allOrganiseringerContainingId = new ConcurrentHashMap<>(16, 0.9f, 1);

    private void putOrUpdateAllOrganiseringer(OrganiseringState organiseringState) {
        String id = organiseringState.id();
        String idOver = organiseringState.idOver();

        if (id == null || idOver == null) {
            logger.warn("ID-felt mangler verdi: {}", organiseringState);
            return;
        }

        if (allOrganiseringerContainingId.containsKey(id)) {
            var connectedOrgEnheter = allOrganiseringerContainingId.get(id);

            var connectedOrgEnheterWithSameId = connectedOrgEnheter.stream().filter(o -> o.id().equals(id)).toList();
            if (!connectedOrgEnheterWithSameId.isEmpty()) {
                logger.warn("Forsøker å legge til {}, men ID '{}' inneholder allerede {}", organiseringState, id, connectedOrgEnheterWithSameId);
            } else {
                connectedOrgEnheter.add(organiseringState);
            }
        } else {
            allOrganiseringerContainingId.put(id, new HashSet<>(List.of(organiseringState)));
        }

        if (allOrganiseringerContainingId.containsKey(idOver)) {
            allOrganiseringerContainingId.get(idOver).add(organiseringState);
        } else {
            allOrganiseringerContainingId.put(idOver, new HashSet<>(List.of(organiseringState)));
        }
    }

    public Optional<OrganiseringState> findByFid(String fid) {
        return Optional.ofNullable(organiseringByFid.get(fid));
    }

    public Optional<OrganiseringState> findActiveByFid(String fid) {
        return getActiveOrganisering().containsKey(fid) ? Optional.ofNullable(getActiveOrganisering().get(fid)) : Optional.empty();
    }

    public Optional<OrganiseringState> findActiveById(String id) {
        var organisering = organiseringById.get(id);

        if (organisering != null) {
            return isActive(organisering) ? Optional.of(organisering) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    public Optional<OrganiseringState> findFidByAgressoIdNivaAndName(Optional<IOrgenhet> orgEnhet) {
        return orgEnhet.flatMap(iOrgenhet -> organiseringByFid.values()
            .stream()
            .filter(organiseringState -> organiseringState.id().equals(iOrgenhet.getId()))
            .findFirst());
    }

    public void clear() {
        organiseringByFid.clear();
        organiseringById.clear();
        allOrganiseringerContainingId.clear();
    }

    public void putEventAggregate(OrganiseringState organiseringState) {
        organiseringByFid.put(organiseringState.fid(), organiseringState);
        organiseringById.put(organiseringState.id(), organiseringState);

        putOrUpdateAllOrganiseringer(organiseringState);
    }

    public int count() {
        return organiseringByFid.size();
    }

    public Map<String, OrganiseringState> getActiveOrganisering() {
        // either terminating date is not present of it is today or later
        return organiseringByFid.entrySet().stream()
            .filter(e -> isActive(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<OrganiseringState> getOrganiseringer() {
        return getActiveOrganisering().values().stream().toList();
    }

    public List<OrganiseringState> getOrganiseringerContainingId(String id) {
        var organiseringerWithId = allOrganiseringerContainingId.get(id);

        if (organiseringerWithId != null) {
            return organiseringerWithId.stream()
                .filter(this::isActive)
                .toList();
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isActive(OrganiseringState organiseringState) {
        return isGyldigTomNull(organiseringState) || (!isCreatedAndDeletedToday(organiseringState) && isGyldigTomTodayOrLater(organiseringState));
    }

    private boolean isCreatedAndDeletedToday(OrganiseringState organiseringState) {
        return organiseringState.gyldigTom().isPresent() &&
            organiseringState.gyldigFom() != null &&
            organiseringState.gyldigTom().get().isEqual(organiseringState.gyldigFom()) &&
            organiseringState.gyldigTom().get().isEqual(LocalDate.now());
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
