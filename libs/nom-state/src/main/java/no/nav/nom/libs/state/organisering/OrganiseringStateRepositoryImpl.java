package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
public class OrganiseringStateRepositoryImpl implements OrganiseringStateRepository {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, OrganiseringState> organiseringByFid = new ConcurrentHashMap<>(16, 0.9f, 1);
    private final Map<String, OrganiseringState> organiseringById = new ConcurrentHashMap<>(16, 0.9f, 1);   // Den siste registrerte OrganiseringState event for orgEnhet id
    private final Map<String, List<OrganiseringState>> organiseringerById = new ConcurrentHashMap<>(16, 0.9f, 1);   // Alle organiseringer for orgEnhet id
    private final Map<String, List<OrganiseringState>> allOrganiseringerContainingId = new ConcurrentHashMap<>(16, 0.9f, 1);

    @Override
    public Optional<OrganiseringState> findByFid(String fid) {
        return Optional.ofNullable(organiseringByFid.get(fid));
    }

    @Override
    public Optional<OrganiseringState> findActiveByFid(String fid) {
        var organisering = organiseringByFid.get(fid);
        if (organisering != null) {
            return isActive(organisering) ? Optional.of(organisering) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<OrganiseringState> findActiveById(String id) {
        var organiseringer = organiseringerById.getOrDefault(id, Collections.emptyList());
        return organiseringer.stream()
            .filter(this::isActive)
            .findFirst();
    }

    @Override
    public List<OrganiseringState> findById(String id) {
        return organiseringerById.getOrDefault(id, Collections.emptyList());
    }

    @Override
    public void clear() {
        organiseringByFid.clear();
        organiseringById.clear();
        organiseringerById.clear();
        allOrganiseringerContainingId.clear();
    }

    @Override
    public void update(OrganiseringState organiseringState) {
        putEventAggregate(organiseringState);
    }

    @Override
    public void putEventAggregate(OrganiseringState organiseringState) {
        organiseringByFid.put(organiseringState.fid(), organiseringState);
        organiseringById.put(organiseringState.id(), organiseringState);

        putOrUpdateOrganiseringerForId(organiseringState);
        putOrUpdateAllOrganiseringer(organiseringState);
    }

    private void putOrUpdateAllOrganiseringer(OrganiseringState newOrganisering) {
        String id = newOrganisering.id();
        String idOver = newOrganisering.idOver();

        if (id == null || idOver == null) {
            logger.warn("ID-felt mangler verdi: {}", newOrganisering);
            return;
        }

        if (allOrganiseringerContainingId.containsKey(id)) {
            var organiseringerContainingId = allOrganiseringerContainingId.get(id);
            var connectedOrganiseringerWithSameId = organiseringerContainingId.stream()
                .filter(o -> o.id().equals(id))
                .toList();

            if (!connectedOrganiseringerWithSameId.isEmpty()) {
                logger.trace("Forsøker å legge til {}, men key '{}' inneholder allerede {}", newOrganisering, id, connectedOrganiseringerWithSameId);
                organiseringerContainingId.removeAll(connectedOrganiseringerWithSameId);
                organiseringerContainingId.add(newOrganisering);
            } else {
                logger.trace("Key {} legger til {}", id, newOrganisering);
                organiseringerContainingId.add(newOrganisering);
            }
        } else {
            logger.trace("Oppretter key {} med {}", id, newOrganisering);
            allOrganiseringerContainingId.put(id, new CopyOnWriteArrayList<>(List.of(newOrganisering)));
        }

        if (allOrganiseringerContainingId.containsKey(idOver)) {
            var organiseringerContainingIdOver = allOrganiseringerContainingId.get(idOver);
            var identicalOrganiseringer = organiseringerContainingIdOver.stream()
                .filter(organisering -> organisering.id().equals(id) && organisering.idOver().equals(idOver))
                .toList();

            if (!identicalOrganiseringer.isEmpty()) {
                logger.trace("Forsøker å legge til {}, men key '{}' inneholder allerede {}", newOrganisering, idOver, identicalOrganiseringer);
                organiseringerContainingIdOver.removeAll(identicalOrganiseringer);
                organiseringerContainingIdOver.add(newOrganisering);
            } else {
                logger.trace("Key {} legger til {}", idOver, newOrganisering);
                allOrganiseringerContainingId.get(idOver).add(newOrganisering);
            }
        } else {
            logger.trace("Oppretter key {} med {}", idOver, newOrganisering);
            allOrganiseringerContainingId.put(idOver, new CopyOnWriteArrayList<>(List.of(newOrganisering)));
        }
    }

    private void putOrUpdateOrganiseringerForId(OrganiseringState organiseringState) {
        if (organiseringerById.containsKey(organiseringState.id())) {
            var organiseringerForId = organiseringerById.get(organiseringState.id());
            var organiseringForSammeFid = organiseringerForId.stream()
                .filter(o -> o.fid().equals(organiseringState.fid()))
                .findFirst();
            if (organiseringForSammeFid.isEmpty()) {
                organiseringerById.put(organiseringState.id(), new CopyOnWriteArrayList<>(List.of(organiseringState)));
            } else {
                organiseringerForId.remove(organiseringForSammeFid.get());
                organiseringerForId.add(organiseringState);
            }
        } else {
            organiseringerById.put(organiseringState.id(), new CopyOnWriteArrayList<>(List.of(organiseringState)));
        }
    }

    @Override
    public void removeEventAggregate(OrganiseringState organiseringState) {
        organiseringByFid.remove(organiseringState.fid());
        organiseringById.forEach((key, value) -> {
            if (value.fid().equals(organiseringState.fid())) {
                organiseringById.remove(key);
            }
        });
        allOrganiseringerContainingId.forEach((key, value) -> value.forEach(state -> {
            if (state.fid().equals(organiseringState.fid())) {
                // delete the matched element from list and update allOrganiseringerContainingId with new list
                // if list become empty then remove the key from allOrganiseringerContainingId
                List<OrganiseringState> matchedList = new CopyOnWriteArrayList<>(value);
                matchedList.remove(state);
                if(matchedList.isEmpty()) {
                    allOrganiseringerContainingId.remove(key);
                } else {
                    allOrganiseringerContainingId.put(key, matchedList);
                }
            }
        }));
        organiseringerById.forEach((key, value) -> value.forEach(state -> {
            if (state.id().equals(organiseringState.id())) {
                // delete the matched element from list and update organiseringerById with new list
                // if list become empty then remove the key from allOrganiseringerContainingId
                List<OrganiseringState> matchedList = new CopyOnWriteArrayList<>(value);
                matchedList.remove(state);
                if(matchedList.isEmpty()) {
                    organiseringerById.remove(key);
                } else {
                    organiseringerById.put(key, matchedList);
                }
            }
        }));
    }

    @Override
    public int count() {
        return organiseringByFid.size();
    }

    @Override
    public void logRepositoryState() {
        if (!logger.isDebugEnabled()) return;

        logger.debug("organiseringByFid: {} organiseringById: {} allOrganiseringerContainingId: {} Aktive organiseringer: {}", organiseringByFid.size(), organiseringById.size(), allOrganiseringerContainingId.size(), getActiveOrganisering().size());

        // Note: detailed information about organiseringByFid, organiseringById and allOrganiseringerContainingId is logged at TRACE level
        try {
            StringBuilder message = new StringBuilder("Diff: difference_organiseringByFid_organiseringById ");
            for (String fid: organiseringByFid.keySet()) {
                OrganiseringState organiseringState = organiseringByFid.get(fid);
                String id = organiseringState.id();
                if(!organiseringById.containsKey(id)) {
                    message.append(" id: ").append(id).append(" in organiseringByFid is not in organiseringById ");
                }
            }
            logger.trace(message.toString());

            StringBuilder message1 = new StringBuilder("Diff: difference_allOrganiseringerContainingId_organiseringById ");
            for (String id: allOrganiseringerContainingId.keySet()) {
                if(!organiseringById.containsKey(id)) {
                    message1.append(" id: ").append(id).append(" in allOrganiseringerContainingId is not in organiseringById ");
                }
            }
            logger.trace(message1.toString());

            StringBuilder message2= new StringBuilder("Diff: difference_allOrganiseringerContainingId_organiseringByFid ");
            for (String id: organiseringById.keySet()) {
                int count = 0;
                List<String> fids = new ArrayList<>();
                for (String fid: organiseringByFid.keySet()) {
                    OrganiseringState organiseringState = organiseringByFid.get(fid);
                    if(organiseringState.id().equals(id)) {
                        count ++;
                        fids.add(fid);
                    }
                }
                if(count > 1) {
                    message2.append(" id: ").append(id).append(" in organiseringById duplicate in organiseringByFid ").append(fids);
                }
            }
            logger.trace(message2.toString());

        } catch (Exception e) {
            // trace logging errors
            logger.trace(e.getLocalizedMessage());
        }
    }

    @Override
    public Map<String, OrganiseringState> getActiveOrganisering() {
        // either terminating date is not present of it is today or later
        return organiseringByFid.entrySet().stream()
            .filter(e -> isActive(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<OrganiseringState> getOrganiseringer() {
        return getActiveOrganisering().values().stream().toList();
    }

    @Override
    public List<OrganiseringState> getOrganiseringerContainingId(String id) {
        return getOrganiseringerContainingId(id, true);
    }

    @Override
    public List<OrganiseringState> getOrganiseringerContainingId(String id, boolean kunAktive) {
        var organiseringerWithId = allOrganiseringerContainingId.get(id);

        if (organiseringerWithId != null) {
            if(kunAktive) {
                return organiseringerWithId.stream()
                    .filter(this::isActive)
                    .toList();
            } else {
                return organiseringerWithId;
            }
        } else {
            return Collections.emptyList();
        }
    }


    @Override
    public Map<String, OrganiseringState> getActiveOrganiseringInBulk(List<String> ids) {
        Set<String> idSet = new HashSet<>(ids);
        return getActiveOrganisering().entrySet().stream()
            .filter(entry -> idSet.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public boolean orgEnhetHasParent(String id) {
        return findActiveById(id)
                .filter(organiseringState -> !ObjectUtils.isEmpty(organiseringState.idOver()))
                .isPresent();
    }
    
    @Override
    public boolean orgEnhetHasChildren(String id) {
        return getOrganiseringerContainingId(id).stream()
                .anyMatch(organiseringState -> organiseringState.idOver().equals(id));
    }

    private boolean isActive(OrganiseringState organiseringState) {
        return isGyldigTomNull(organiseringState) || (!isCreatedAndDeletedToday(organiseringState) && isGyldigTomTodayOrLater(organiseringState));
    }

    private boolean isCreatedAndDeletedToday(OrganiseringState organiseringState) {
        return nonNull(organiseringState.gyldigTom()) &&
            nonNull(organiseringState.gyldigFom()) &&
            organiseringState.gyldigTom().isEqual(organiseringState.gyldigFom()) &&
            organiseringState.gyldigTom().isEqual(LocalDate.now());
    }

    private boolean isGyldigTomTodayOrLater(OrganiseringState organiseringState) {
        return nonNull(organiseringState.gyldigTom())
            && organiseringState.gyldigTom().isAfter(LocalDate.now().minusDays(1));
    }

    private boolean isGyldigTomNull(OrganiseringState organiseringState) {
        LocalDate gyldigTom = Optional.of(organiseringState)
            .filter(state -> nonNull(state.gyldigTom()))
            .map(OrganiseringState::gyldigTom)
            .orElse(null);
        return gyldigTom == null;
    }
}
