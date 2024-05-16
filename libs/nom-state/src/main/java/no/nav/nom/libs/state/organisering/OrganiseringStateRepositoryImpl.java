package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class OrganiseringStateRepositoryImpl implements OrganiseringStateRepository {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, OrganiseringState> organiseringByFid = new ConcurrentHashMap<>(16, 0.9f, 1);
    private final Map<String, OrganiseringState> organiseringById = new ConcurrentHashMap<>(16, 0.9f, 1);
    private final Map<String, List<OrganiseringState>> allOrganiseringerContainingId = new ConcurrentHashMap<>(16, 0.9f, 1);

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
            allOrganiseringerContainingId.put(id, new ArrayList<>(List.of(newOrganisering)));
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
            allOrganiseringerContainingId.put(idOver, new ArrayList<>(List.of(newOrganisering)));
        }
    }

    @Override
    public Optional<OrganiseringState> findByFid(String fid) {
        return Optional.ofNullable(organiseringByFid.get(fid));
    }

    @Override
    public Optional<OrganiseringState> findActiveByFid(String fid) {
        return getActiveOrganisering().containsKey(fid) ? Optional.ofNullable(getActiveOrganisering().get(fid)) : Optional.empty();
    }

    @Override
    public Optional<OrganiseringState> findActiveById(String id) {
        var organisering = organiseringById.get(id);

        if (organisering != null) {
            return isActive(organisering) ? Optional.of(organisering) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<OrganiseringState> findById(String id) {
        return Optional.ofNullable(organiseringById.get(id));
    }

    @Override
    public void clear() {
        organiseringByFid.clear();
        organiseringById.clear();
        allOrganiseringerContainingId.clear();
    }

    @Override
    public void update(OrganiseringState organiseringState) {
        if(organiseringState.gyldigTom() != null && organiseringState.gyldigTom().isPresent() && organiseringState.gyldigTom().get().isBefore(LocalDate.now())){
            removeEventAggregate(organiseringState);
        } else {
            putEventAggregate(organiseringState);
        }
    }

    @Override
    public void putEventAggregate(OrganiseringState organiseringState) {
        organiseringByFid.put(organiseringState.fid(), organiseringState);
        organiseringById.put(organiseringState.id(), organiseringState);

        putOrUpdateAllOrganiseringer(organiseringState);
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
    }

    @Override
    public int count() {
        return organiseringByFid.size();
    }

    @Override
    public void logRepositoryState() {
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
