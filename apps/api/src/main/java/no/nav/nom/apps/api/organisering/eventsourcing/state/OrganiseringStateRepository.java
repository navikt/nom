package no.nav.nom.apps.api.organisering.eventsourcing.state;

import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrganiseringStateRepository {

    private final OrgenhetService orgenhetService;

    private final Map<String, OrganiseringState> organiseringByFid = new ConcurrentHashMap<>();

    public OrganiseringStateRepository(OrgenhetService orgenhetService) {
        this.orgenhetService = orgenhetService;
    }

    public void put(String fid, OrganiseringState organiseringState) {
        organiseringByFid.put(fid, organiseringState);
    }

    public Optional<OrganiseringState> findByFid(String fid) {
        return Optional.ofNullable(organiseringByFid.get(fid));
    }

    public String findFidByAgressoIdNivaAndName(String agressoId, String niva, String name) {
        var orgEnhet = orgenhetService
            .getEnhetByAgressoIdNivaAndName(agressoId, niva, name);

        return orgEnhet.flatMap(iOrgenhet -> organiseringByFid.entrySet()
            .stream()
            .filter(organiseringInState -> organiseringInState.getValue().idUnder().equals(iOrgenhet.getId()))
            .map(Map.Entry::getKey)
            .findFirst()).orElse(null);
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
}
