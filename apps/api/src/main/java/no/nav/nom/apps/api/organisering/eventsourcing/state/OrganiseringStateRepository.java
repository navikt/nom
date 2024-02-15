package no.nav.nom.apps.api.organisering.eventsourcing.state;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
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
import java.util.stream.Collectors;

@Service
public class OrganiseringStateRepository {

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
                logger.warn("Forsøker å legge til {}, men key '{}' inneholder allerede {}", newOrganisering, id, connectedOrganiseringerWithSameId);
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
                logger.warn("Forsøker å legge til {}, men key '{}' inneholder allerede {}", newOrganisering, idOver, identicalOrganiseringer);
            } else {
                logger.trace("Key {} legger til {}", idOver, newOrganisering);
                allOrganiseringerContainingId.get(idOver).add(newOrganisering);
            }
        } else {
            logger.trace("Oppretter key {} med {}", idOver, newOrganisering);
            allOrganiseringerContainingId.put(idOver, new ArrayList<>(List.of(newOrganisering)));
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

    public void logRepositoryState() {
        logger.debug("organiseringByFid: {} organiseringById: {} allOrganiseringerContainingId: {} Aktive organiseringer: {}", organiseringByFid.size(), organiseringById.size(), allOrganiseringerContainingId.size(), getActiveOrganisering().size());

        // TODO delete the rest of this method. This is just for logging and testing and should be removed
       try {
           StringBuilder message = new StringBuilder("Diff: difference_allOrganiseringerContainingId_organiseringByFid ");
           for (String fid: organiseringByFid.keySet()) {
               OrganiseringState organiseringState = organiseringByFid.get(fid);
               String id = organiseringState.id();
               if(!organiseringById.containsKey(id)) {
                   message.append(" id: ").append(id).append(" in organiseringByFid is not in organiseringById ");
               }
           }
           logger.debug(message.toString());

           StringBuilder message1 = new StringBuilder("Diff: difference_allOrganiseringerContainingId_organiseringById ");
           for (String id: allOrganiseringerContainingId.keySet()) {
               if(!organiseringById.containsKey(id)) {
                   message1.append(" id: ").append(id).append(" in allOrganiseringerContainingId is not in organiseringById ");
               }
           }
           logger.debug(message1.toString());
        } catch (Exception e) {
            //e.printStackTrace();
           // ignore
        }
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
