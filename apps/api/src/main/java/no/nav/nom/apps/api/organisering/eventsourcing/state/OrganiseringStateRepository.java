package no.nav.nom.apps.api.organisering.eventsourcing.state;

import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrganiseringStateRepository {

    private final Map<String, DvhOrganisering> organiseringByFid = new ConcurrentHashMap<>();

    public void put(String fid, DvhOrganisering dvhOrganisering) {
        organiseringByFid.put(fid, dvhOrganisering);
    }

    public Optional<DvhOrganisering> findByFid(String fid){
        return Optional.ofNullable(organiseringByFid.get(fid));
    }

    public String findFidlByUniqueAgressoId(DvhOrganisering dvhOrganisering){
        var result = organiseringByFid.entrySet()
            .stream()
            .filter(dvhOrganiseringInState -> dvhOrganiseringInState.getValue().getAgressoIdUnder().equals(dvhOrganisering.getAgressoIdUnder()))
            .findFirst()
            .orElse(null);

        return (result == null)? null : result.getKey();
    }

}
