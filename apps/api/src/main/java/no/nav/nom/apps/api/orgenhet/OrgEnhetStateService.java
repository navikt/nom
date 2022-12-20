package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrgEnhetStateService {
    private final Map<String, OrgEnhetState> orgEnhetByFid = new ConcurrentHashMap<>();
    private final Map<String, OrgEnhetState> orgEnhetById = new ConcurrentHashMap<>();

    private final Map<String, OrgEnhetState> orgEnhetByUniqueAgressoId = new ConcurrentHashMap<>();

    public Optional<OrgEnhetState> findByFid(String fid) {
        return Optional.ofNullable(orgEnhetByFid.get(fid));
    }

    public Optional<OrgEnhetState> findById(String id) {
        return Optional.ofNullable(orgEnhetByFid.get(id));
    }

    public Optional<OrgEnhetState> findByUniqueAgressoId(String id) {
        return Optional.ofNullable(orgEnhetByUniqueAgressoId.get(id));
    }

    public Collection<OrgEnhetState> getAll() {
        return orgEnhetByFid.values();
    }

    public void putEventAggregate(OrgEnhetState orgEnhetState) {
        orgEnhetByFid.put(orgEnhetState.fid(), orgEnhetState);
        orgEnhetById.put(orgEnhetState.id(), orgEnhetState);
        if (orgEnhetState.uniqueAgressoId().isPresent())
            orgEnhetByUniqueAgressoId.put(orgEnhetState.uniqueAgressoId().get(), orgEnhetState);
    }

    public void clear() {
        orgEnhetByFid.clear();
        orgEnhetById.clear();
        orgEnhetByUniqueAgressoId.clear();
    }
}
