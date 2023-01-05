package no.nav.nom.apps.api.orgenhet.eventsourcing.state;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Comparator.comparing;

@Service
public class OrgEnhetStateRepository {
    private final Map<String, OrgEnhetState> orgEnhetByFid = new ConcurrentHashMap<>();
    private final Map<String, OrgEnhetState> orgEnhetById = new ConcurrentHashMap<>();

    private final Map<String, List<OrgEnhetState>> orgEnheterByUniqueAgressoId = new ConcurrentHashMap<>();

    public Optional<OrgEnhetState> findByFid(String fid) {
        return Optional.ofNullable(orgEnhetByFid.get(fid));
    }

    public Optional<OrgEnhetState> findById(String id) {
        return Optional.ofNullable(orgEnhetByFid.get(id));
    }

    public List<OrgEnhetState> findAllByUniqueAgressoId(String id) {
        List<OrgEnhetState> orgEnheter = orgEnheterByUniqueAgressoId.get(id);
        if (orgEnheter == null) {
            orgEnheter = new ArrayList();
            orgEnheterByUniqueAgressoId.put(id, orgEnheter);
        }
        return orgEnheter;
    }

    public Optional<OrgEnhetState> findNewestUniqueAgressoId(String id) {
        return findAllByUniqueAgressoId(id).stream().max(comparing(OrgEnhetState::gyldigFom));
    }

    public Collection<OrgEnhetState> getAll() {
        return orgEnhetByFid.values();
    }

    public int count() {
        return orgEnhetByFid.size();
    }

    public void putEventAggregate(OrgEnhetState newOrgEnhetState) {
        orgEnhetByFid.put(newOrgEnhetState.fid(), newOrgEnhetState);
        orgEnhetById.put(newOrgEnhetState.id(), newOrgEnhetState);
        addToUniqueQgressoIdList(newOrgEnhetState);
    }

    private void addToUniqueQgressoIdList(OrgEnhetState newOrgEnhetState) {
        if (newOrgEnhetState.uniqueAgressoId().isPresent()) {
            List<OrgEnhetState> orgEnheter = findAllByUniqueAgressoId(newOrgEnhetState.uniqueAgressoId().get());
            var enhet = orgEnheter.stream().filter( orgEnhet -> orgEnhet.fid().equals(newOrgEnhetState.fid())).findAny();
            if (enhet.isPresent()) {
                orgEnheter.remove(enhet.get());
            }
            orgEnheter.add(newOrgEnhetState);
        }
    }

    public void clear() {
        orgEnhetByFid.clear();
        orgEnhetById.clear();
        orgEnheterByUniqueAgressoId.clear();
    }
}
