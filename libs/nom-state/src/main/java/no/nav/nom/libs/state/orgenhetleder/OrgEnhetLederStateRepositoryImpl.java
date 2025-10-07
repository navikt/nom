package no.nav.nom.libs.state.orgenhetleder;

import no.nav.nom.contract.nom.state.OrgEnhetLederState;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;

public class OrgEnhetLederStateRepositoryImpl implements OrgEnhetLederStateRepository {
    private final Map<String, OrgEnhetLederState> orgEnhetLederByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgEnhetLederState>> orgEnhetLederByNavident = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgEnhetLederState>> orgEnhetLederByOrgEnhetId = new ConcurrentHashMap<>();

    @Override
    public Optional<OrgEnhetLederState> findByFid(String fid) {
        return Optional.ofNullable(orgEnhetLederByFid.get(fid));
    }

    @Override
    public List<OrgEnhetLederState> findAllByNavident(String navident) {
        return orgEnhetLederByNavident.getOrDefault(navident, Collections.emptyMap()).values().stream().toList();
    }

    @Override
    public List<OrgEnhetLederState> findAllByOrgEnhetId(String orgEnhetId) {
        return orgEnhetLederByOrgEnhetId.getOrDefault(orgEnhetId, Collections.emptyMap()).values().stream()
            .sorted(Comparator
                    .comparing(OrgEnhetLederState::startdato, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(OrgEnhetLederState::sluttdato, Comparator.nullsLast(LocalDate::compareTo)))
            .toList();
    }

    @Override
    public Optional<OrgEnhetLederState> getActiveForOrgEnhet(String orgEnhetId) {
        return orgEnhetLederByOrgEnhetId.getOrDefault(orgEnhetId, Collections.emptyMap()).values().stream()
            .filter(orgEnhetLederState -> isActive(orgEnhetLederState.startdato(), orgEnhetLederState.sluttdato()))
            .findFirst();
    }

    @Override
    public boolean hasActiveInPeriodByOrgEnhetId(String orgEnhetId, LocalDate startdato, LocalDate sluttdato) {
        return orgEnhetLederByOrgEnhetId.getOrDefault(orgEnhetId, Collections.emptyMap())
            .values()
            .stream()
            .anyMatch(orgEnhetLederState -> isActive(startdato, sluttdato));
    }

    @Override
    public int count() {
        return orgEnhetLederByFid.size();
    }

    @Override
    public void update(String fid, OrgEnhetLederState newOrgEnhetLederState) {
        if (newOrgEnhetLederState == null) {
            removeFromState(fid);
        } else {
            addToState(newOrgEnhetLederState);
        }
    }

    private void addToState(OrgEnhetLederState newOrgEnhetLederState) {
        orgEnhetLederByFid.put(newOrgEnhetLederState.fid(),  newOrgEnhetLederState);

        addToByNavident(newOrgEnhetLederState);
        addToByOrgEnhetId(newOrgEnhetLederState);
    }

    private void removeFromState(String fid) {
        var eksisterende = orgEnhetLederByFid.get(fid);
        if (eksisterende != null) {
            removeFromOrgEnhetLederByNavident(eksisterende);
            removeFromOrgEnhetLederByOrgEnhetId(eksisterende);
        }
        orgEnhetLederByFid.remove(fid);
    }

    @Override
    public void clear() {
        orgEnhetLederByFid.clear();
        orgEnhetLederByNavident.clear();
        orgEnhetLederByOrgEnhetId.clear();
    }

    private void addToByNavident(OrgEnhetLederState newState) {
        var navidentOrgEnheterLeder = orgEnhetLederByNavident.computeIfAbsent(newState.navident(), k -> new ConcurrentHashMap<>());
        navidentOrgEnheterLeder.put(newState.fid(), newState);
    }

    private void addToByOrgEnhetId(OrgEnhetLederState newState) {
        var orgEnhetidOrgEnhetLedere = orgEnhetLederByOrgEnhetId.computeIfAbsent(newState.orgEnhetId(), k -> new ConcurrentHashMap<>());
        orgEnhetidOrgEnhetLedere.put(newState.fid(), newState);
    }

    private void removeFromOrgEnhetLederByNavident(OrgEnhetLederState oldState) {
        var navidentOrgEnhetLeder = orgEnhetLederByNavident.get(oldState.navident());
        if (navidentOrgEnhetLeder != null) {
            navidentOrgEnhetLeder.remove(oldState.fid());
        }
    }

    private void removeFromOrgEnhetLederByOrgEnhetId(OrgEnhetLederState oldState) {
        var orgEnhetIdOrgEnhetLeder = orgEnhetLederByOrgEnhetId.get(oldState.fid());
        if (orgEnhetIdOrgEnhetLeder != null) {
            orgEnhetIdOrgEnhetLeder.remove(oldState.fid());
        }
    }
}
