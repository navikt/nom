package no.nav.nom.apps.api.orgtilknytning.eventsourcing.state;

import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrgtilknytningStateRepository {
    private final Map<String, OrgtilknytningState> orgtilknytningByFid = new ConcurrentHashMap<>();
    private final Map<String, OrgtilknytningState> orgtilknytningByNavident = new ConcurrentHashMap<>();

    public Optional<OrgtilknytningState> findByFid(String fid) {
        return Optional.ofNullable(orgtilknytningByFid.get(fid));
    }

    public List<OrgtilknytningState> findByFidIn(List<String> fids) {
        return orgtilknytningByFid.entrySet().stream()
            .filter( e -> fids.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public Optional<OrgtilknytningState> findByNavident(String navident) {
        return Optional.ofNullable(orgtilknytningByNavident.get(navident));
    }

    public List<OrgtilknytningState> findByNavidentIn(List<String> navidents) {
        return orgtilknytningByNavident.entrySet().stream()
            .filter( e -> navidents.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public Collection<OrgtilknytningState> getAll() {
        return orgtilknytningByFid.values();
    }

    public int count() {
        return orgtilknytningByFid.size();
    }

    public void putEventAggregate(OrgtilknytningState newOrgtilknytningState) {
        orgtilknytningByFid.put(newOrgtilknytningState.fid(), newOrgtilknytningState);
        orgtilknytningByNavident.put(newOrgtilknytningState.navident(), newOrgtilknytningState);
    }

    public void clear() {
        orgtilknytningByFid.clear();
        orgtilknytningByNavident.clear();
    }

}
