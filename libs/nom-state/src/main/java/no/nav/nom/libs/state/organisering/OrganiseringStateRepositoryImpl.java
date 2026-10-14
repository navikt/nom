package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
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

    private final Map<String, OrganiseringState> organiseringByFid = new ConcurrentHashMap<>();
    private final Map<String, List<OrganiseringState>> organiseringerById = new ConcurrentHashMap<>();   // Alle organiseringer for orgEnhet id
    private final Map<String, List<OrganiseringState>> allOrganiseringerContainingId = new ConcurrentHashMap<>();

    private final Map<String, Set<OrganiseringState>> enhetOgOrganiseringerNedover = new ConcurrentHashMap<>(); // Alle organiseringer for orgEnhetene i organiseringen nedover
    private final Map<String, Set<OrganiseringState>> enhetOgOrganiseringerOppover = new ConcurrentHashMap<>(); // Alle organiseringer for orgEnheten og alle organiseringer for orgEnhetene i organiseringen oppover

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
        organiseringerById.clear();
        allOrganiseringerContainingId.clear();
    }

    @Override
    public void update(OrganiseringState organiseringState) {
        if (!isValid(organiseringState)) {
            return;
        }

        // Behandle organisesringer med lik start og sluttdato som slettet
        if (isCreatedAndDeletedToday(organiseringState)) {
            removeEventAggregate(organiseringState);
        } else {
            putEventAggregate(organiseringState);
        }
    }

    private boolean isValid(OrganiseringState organiseringState) {
        if (ObjectUtils.isEmpty(organiseringState.fid())) {
            logger.error("OrganiseringState mangler 'fid': {}", organiseringState);
            return false;
        }
        if (ObjectUtils.isEmpty(organiseringState.id())) {
            logger.error("OrganiseringState mangler 'id': {}", organiseringState);
            return false;
        }
        if (ObjectUtils.isEmpty(organiseringState.idOver())) {
            logger.error("OrganiseringState mangler 'idOver': {}", organiseringState);
            return false;
        }
        if(organiseringState.gyldigFom() != null && organiseringState.gyldigTom() != null) {
            if(organiseringState.gyldigFom().isAfter(organiseringState.gyldigTom())) {
                logger.error("OrganiseringState har ugyldige datoer: {}", organiseringState);
            }
        }
        return true;
    }

    public void putEventAggregate(OrganiseringState organiseringState) {
        organiseringByFid.put(organiseringState.fid(), organiseringState);

        putOrUpdateOrganiseringerForId(organiseringState);
        putOrUpdateAllOrganiseringer(organiseringState);

        // Må etter de to over
        putOrUpdateEnhetOgOrganiseringerNedover(organiseringState);
        putOrUpdateEnhetOgOrganiseringerOppover(organiseringState);
    }

    private void putOrUpdateAllOrganiseringer(OrganiseringState newOrganisering) {
        String id = newOrganisering.id();
        String idOver = newOrganisering.idOver();

        if (id == null || idOver == null) {
            logger.warn("ID-felt mangler verdi: {}", newOrganisering);
            return;
        }

        allOrganiseringerContainingId.computeIfAbsent(id, _ -> new CopyOnWriteArrayList<>());
        var organiseringerContainingId = allOrganiseringerContainingId.get(id);

        // Finnes samme FID - så bytt ut med ny state
        organiseringerContainingId.stream()
            .filter(o -> o.fid().equals(newOrganisering.fid()))
            .findFirst()
            .ifPresent(organiseringerContainingId::remove);

        organiseringerContainingId.add(newOrganisering);

        allOrganiseringerContainingId.computeIfAbsent(idOver, _ -> new CopyOnWriteArrayList<>());
        var organiseringerContainingIdOver = allOrganiseringerContainingId.get(idOver);

        // Finnes samme FID - så bytt ut med ny state
        organiseringerContainingIdOver.stream()
            .filter(o -> o.fid().equals(newOrganisering.fid()))
            .findFirst()
            .ifPresent(organiseringerContainingIdOver::remove);
        organiseringerContainingIdOver.add(newOrganisering);
    }

    private void putOrUpdateOrganiseringerForId(OrganiseringState organiseringState) {
        if (organiseringerById.containsKey(organiseringState.id())) {
            var organiseringerForId = organiseringerById.get(organiseringState.id());
            var organiseringForSammeFid = organiseringerForId.stream()
                .filter(o -> o.fid().equals(organiseringState.fid()))
                .findFirst();
            if (organiseringForSammeFid.isEmpty()) {
                organiseringerById.get(organiseringState.id()).add(organiseringState);
            } else {
                organiseringerForId.remove(organiseringForSammeFid.get());
                organiseringerForId.add(organiseringState);
            }
        } else {
            organiseringerById.put(organiseringState.id(), new CopyOnWriteArrayList<>(List.of(organiseringState)));
        }
    }

    private void putOrUpdateEnhetOgOrganiseringerNedover(OrganiseringState organiseringState) {
        // Vi kan få flere State objekter for samme FID (kafka compacted cleanup har ikke fjernet gamle)
        // Sjekk om vi har denne staten i dette settet ifra før, isåfall fjern den gamle
        removeStateWithSameFidFromAllNedoverMaps(organiseringState);

        // Denne organiseringen kan peke oppover på meg
        // -> jeg må da legge til alle organiseringer som inneholder idOver i nedover-kartet for id
        var meg = organiseringState.idOver();

        var alleMineNedoverOrganiseringer = enhetOgOrganiseringerNedover.computeIfAbsent(meg, _ -> new HashSet<>());

        Set<OrganiseringState> mineNedoverOrganiseringerFraDenneStaten = new HashSet<>();
        mineNedoverOrganiseringerFraDenneStaten.add(organiseringState);

        addStateBelowRecursively(mineNedoverOrganiseringerFraDenneStaten, organiseringState.id());

        // Ekstra guard: sikrer at ingen gammel versjon av samme FID blir liggende i dette konkrete settet
        // Dedupe i målsettet, ikke i kildesettet
        var fider = mineNedoverOrganiseringerFraDenneStaten.stream()
            .map(OrganiseringState::fid)
            .collect(Collectors.toSet());
        alleMineNedoverOrganiseringer.removeIf(existing -> fider.contains(existing.fid()));

        alleMineNedoverOrganiseringer.addAll(mineNedoverOrganiseringerFraDenneStaten);

        // -> Og de over meg må få mitt nedoverkart
        addMineNedoverStatesTilParentsRecursively(meg, alleMineNedoverOrganiseringer);
    }

    private void addMineNedoverStatesTilParentsRecursively(String meg, Set<OrganiseringState> mineNedoverOrganiseringerFraDenneStaten) {
        findById(meg).forEach(parentOrganisering -> {
            var minParent = parentOrganisering.idOver();
            // legg mine under-organiseringer hos min parent

            var parentSet = enhetOgOrganiseringerNedover.computeIfAbsent(minParent, _ -> new HashSet<>());

            // Fjern stale varianter for samme FID før vi legger inn nyeste versjoner
            var fider = mineNedoverOrganiseringerFraDenneStaten.stream()
                .map(OrganiseringState::fid)
                .collect(Collectors.toSet());
            parentSet.removeIf(existing -> fider.contains(existing.fid()));

            parentSet.addAll(mineNedoverOrganiseringerFraDenneStaten);

            addMineNedoverStatesTilParentsRecursively(minParent, mineNedoverOrganiseringerFraDenneStaten);
        });
    }

    private void addStateBelowRecursively(Set<OrganiseringState> organiseringer, String id) {
        allOrganiseringerContainingId.get(id).stream()
            .filter(organisering -> organisering.idOver().equals(id))
            .forEach(o ->{
                if(organiseringer.add(o)) {
                    addStateBelowRecursively(organiseringer, o.id());
                }
            });
    }

    private void putOrUpdateEnhetOgOrganiseringerOppover(OrganiseringState organiseringState) {
        // Vi kan få flere State objekter for samme FID (kafka compacted cleanup har ikke fjernet gamle)
        // Sjekk om vi har denne staten i dette settet ifra før, isåfall fjern den gamle
        removeStateWithSameFidFromAllOppoverMaps(organiseringState);

        // Denne organiseringen kan peke nedover på meg
        // -> jeg må da legge til alle organiseringer som inneholder idOver i oppover-kartet for id
        var meg = organiseringState.id();

        var alleMineOppoverOrganiseringer = enhetOgOrganiseringerOppover.computeIfAbsent(meg, _ -> new HashSet<>());

        Set<OrganiseringState> mineOppoverOrganiseringerFraDenneStaten = new HashSet<>();
        mineOppoverOrganiseringerFraDenneStaten.add(organiseringState);

        addStateAboveRecursively(mineOppoverOrganiseringerFraDenneStaten, organiseringState.idOver());

        // Ekstra guard: sikrer at ingen gammel versjon av samme FID blir liggende i dette konkrete settet
        // Dedupe i målsettet, ikke i kildesettet
        var fider = mineOppoverOrganiseringerFraDenneStaten.stream()
            .map(OrganiseringState::fid)
            .collect(Collectors.toSet());
        alleMineOppoverOrganiseringer.removeIf(existing -> fider.contains(existing.fid()));

        alleMineOppoverOrganiseringer.addAll(mineOppoverOrganiseringerFraDenneStaten);

        // -> Og de over meg må få mitt oppoverkart
        addMineOppoverStatesTilChildrenRecursively(meg, alleMineOppoverOrganiseringer);
    }

    private void addMineOppoverStatesTilChildrenRecursively(String meg, Set<OrganiseringState> mineOppoverOrganiseringerFraDenneStaten) {
        allOrganiseringerContainingId.get(meg).stream()
                .filter(organisering -> organisering.idOver().equals(meg))
                .forEach(childOrganisering -> {
                    var minChild = childOrganisering.id();


                    var childSet = enhetOgOrganiseringerOppover.computeIfAbsent(minChild, _ -> new HashSet<>());

                    // Fjern stale varianter for samme FID før vi legger inn nyeste versjoner
                    var fider = mineOppoverOrganiseringerFraDenneStaten.stream()
                        .map(OrganiseringState::fid)
                        .collect(Collectors.toSet());
                    childSet.removeIf(existing -> fider.contains(existing.fid()));

                    childSet.addAll(mineOppoverOrganiseringerFraDenneStaten);

                    addMineOppoverStatesTilChildrenRecursively(minChild, mineOppoverOrganiseringerFraDenneStaten);
                });
    }

    private void removeStateWithSameFidFromAllNedoverMaps(OrganiseringState newState) {
        enhetOgOrganiseringerNedover.values()
            .forEach(states -> states.removeIf(existing -> existing.fid().equals(newState.fid())));
    }

    private void removeStateWithSameFidFromAllOppoverMaps(OrganiseringState newState) {
        enhetOgOrganiseringerOppover.values()
            .forEach(states -> states.removeIf(existing -> existing.fid().equals(newState.fid())));
    }

    private void addStateAboveRecursively(Set<OrganiseringState> organiseringer, String idOver) {
        allOrganiseringerContainingId.get(idOver).stream()
            .filter(organisering -> organisering.id().equals(idOver))
            .forEach(o ->{
                if(organiseringer.add(o)) {
                    addStateAboveRecursively(organiseringer, o.idOver());
                }
            });
    }

    public void removeEventAggregate(OrganiseringState organiseringState) {
        organiseringByFid.remove(organiseringState.fid());
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

        enhetOgOrganiseringerNedover.forEach((_, organiseringer) -> organiseringer.remove(organiseringState));
        enhetOgOrganiseringerOppover.forEach((_, organiseringer) -> organiseringer.remove(organiseringState));
    }

    @Override
    public int count() {
        return organiseringByFid.size();
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
    public Map<String, OrganiseringState> getActiveOrganiseringInBulk(Set<String> ids) {
        return getActiveOrganisering().entrySet().stream()
            .filter(entry -> ids.contains(entry.getKey()))
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

    @Override
    public Set<OrganiseringState> getEnhetOgOrganiseringerNedover(String id) {
        return enhetOgOrganiseringerNedover.getOrDefault(id, Collections.emptySet());
    }

    @Override
    public Set<OrganiseringState> getEnhetOgOrganiseringerOppover(String id) {
        return enhetOgOrganiseringerOppover.getOrDefault(id, Collections.emptySet());
    }

    @Override
    public Set<String> getChildrenEnhetIdsFlattened(String id) {
        return getEnhetOgOrganiseringerNedover(id).stream()
            .map(OrganiseringState::id)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getParentEnhetIdsFlattened(String id) {
        return getEnhetOgOrganiseringerOppover(id).stream()
            .map(OrganiseringState::id)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAlleLovnodeIder() {
        var alleOverenhetIder = organiseringByFid.values().stream().map(OrganiseringState::idOver).collect(Collectors.toSet());
        var alleEnheterMedOrganisering = organiseringByFid.values().stream().map(OrganiseringState::id).collect(Collectors.toSet());
        return alleEnheterMedOrganisering.stream()
            .filter(id -> !alleOverenhetIder.contains(id))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<OrganiseringState> getLovnoderUnder(String id) {
        var lovnoderIds = getLovnoderIderUnder(id);
        return getEnhetOgOrganiseringerNedover(id).stream()
            .filter(organisering  -> lovnoderIds.contains(organisering.id()))
            .collect(Collectors.toSet());
    }

    // NB! Merk at hvis en enhet har i en gitt periode hatt underenheter, og i annen periode ikke hatt underenheter
    // så vil den ikke defineres som en løvnode her - for enkelhetsskyld
    @Override
    public Set<String> getLovnoderIderUnder(String id) {
        var childrenIds = getChildrenEnhetIdsFlattened(id);
        var organiseringerUnderSineOverIder = getEnhetOgOrganiseringerNedover(id).stream()
            .map(OrganiseringState::idOver)
            .collect(Collectors.toSet());
        return childrenIds.stream()
            .filter(childId -> !organiseringerUnderSineOverIder.contains(childId))
            .collect(Collectors.toSet());
    }

    public boolean isActive(OrganiseringState organiseringState) {
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
