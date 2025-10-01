package no.nav.nom.libs.state.orgenhetleder;

import no.nav.nom.contract.nom.state.OrgEnhetLederState;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;

public class OrgEnhetLederStateRepositoryImpl implements OrgEnhetLederStateRepository {
    private final Map<String, OrgEnhetLederState> orgEnhetLederByFid = new ConcurrentHashMap<>();
    private final Map<String, List<OrgEnhetLederState>> orgEnhetLederByNavident = new ConcurrentHashMap<>();
    private final Map<String, List<OrgEnhetLederState>> orgEnhetLederByOrgEnhetId = new ConcurrentHashMap<>();

    @Override
    public Optional<OrgEnhetLederState> findByFid(String fid) {
        return Optional.ofNullable(orgEnhetLederByFid.get(fid));
    }

    @Override
    public List<OrgEnhetLederState> findByNavident(String navident) {
        return orgEnhetLederByNavident.getOrDefault(navident, Collections.emptyList());
    }

    @Override
    public List<OrgEnhetLederState> findByOrgEnhetId(String orgEnhetId) {
        return orgEnhetLederByOrgEnhetId.getOrDefault(orgEnhetId, Collections.emptyList()).stream()
            .sorted(Comparator
                    .comparing(OrgEnhetLederState::startdato, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(OrgEnhetLederState::sluttdato, Comparator.nullsLast(LocalDate::compareTo)))
            .toList();
    }

    @Override
    public Optional<OrgEnhetLederState> getActiveForOrgEnhet(String orgEnhetId) {
        return orgEnhetLederByOrgEnhetId.get(orgEnhetId).stream()
            .filter(orgEnhetLederState -> isActive(orgEnhetLederState.startdato(), orgEnhetLederState.sluttdato()))
            .findFirst();
    }

    @Override
    public boolean hasActiveInPeriodByOrgEnhetId(String orgEnhetId, LocalDate startdato, LocalDate sluttdato) {
        return orgEnhetLederByOrgEnhetId.get(orgEnhetId).stream().anyMatch(orgEnhetLederState -> isActive(startdato, sluttdato));
    }

    @Override
    public int count() {
        return orgEnhetLederByFid.size();
    }

    @Override
    public void update(String fid, OrgEnhetLederState state) {
        var oldState = orgEnhetLederByFid.get(fid);
        orgEnhetLederByFid.put(fid, state);

        if (oldState != null) {
            removeFromOrgEnhetLederByNavident(oldState);
            removeFromOrgEnhetLederByOrgEnhetId(oldState);
        }

        addToByNavident(state);
        addToByOrgEnhetId(state);
    }

    @Override
    public void clear() {
        orgEnhetLederByFid.clear();
        orgEnhetLederByNavident.clear();
        orgEnhetLederByOrgEnhetId.clear();
    }

    private void addToByNavident(OrgEnhetLederState newState) {
        orgEnhetLederByNavident.computeIfAbsent(newState.navident(), k -> new CopyOnWriteArrayList<>()).add(newState);
    }

    private void addToByOrgEnhetId(OrgEnhetLederState newState) {
        orgEnhetLederByOrgEnhetId.computeIfAbsent(newState.orgEnhetId(), k -> new CopyOnWriteArrayList<>()).add(newState);
    }

    private void removeFromOrgEnhetLederByNavident(OrgEnhetLederState oldState) {
        orgEnhetLederByNavident.getOrDefault(oldState.navident(), Collections.emptyList()).remove(oldState);
    }

    private void removeFromOrgEnhetLederByOrgEnhetId(OrgEnhetLederState oldState) {
        orgEnhetLederByOrgEnhetId.remove(oldState.orgEnhetId());
    }
}
