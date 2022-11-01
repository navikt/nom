package no.nav.nom.apps.api.eventsourcing.state;

import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KafkaStateRessursService {
    private final Map<UUID, RessursState> ressursByFid = new ConcurrentHashMap<>();
    private final Map<String, RessursState> ressursByPersonident = new ConcurrentHashMap<>();
    public Optional<RessursState> findByFid(UUID fid) {
        return Optional.ofNullable(ressursByFid.get(fid));
    }

    public Optional<RessursState> findByPersonident(String personident) {
        return Optional.ofNullable(ressursByPersonident.get(personident));
    }

    public Collection<RessursState> getAll() {
        return ressursByFid.values();
    }

    public void putEventAggregate(RessursState ressursEventData){
        ressursByFid.put(ressursEventData.fid(), ressursEventData);
        ressursByPersonident.put(ressursEventData.personident(), ressursEventData);
    }
}
