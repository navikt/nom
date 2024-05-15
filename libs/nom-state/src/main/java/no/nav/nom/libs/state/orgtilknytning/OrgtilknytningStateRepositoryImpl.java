package no.nav.nom.libs.state.orgtilknytning;

import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrgtilknytningStateRepositoryImpl implements OrgtilknytningStateRepository {
    private final Map<String, OrgtilknytningState> orgtilknytningByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgtilknytningState>> orgtilknytningerByNavident = new ConcurrentHashMap<>();

    @Override
    public Optional<OrgtilknytningState> findByFid(String fid) {
        return Optional.ofNullable(orgtilknytningByFid.get(fid));
    }

    @Override
    public List<OrgtilknytningState> findByFidIn(List<String> fids) {
        return orgtilknytningByFid.entrySet().stream()
            .filter(e -> fids.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<OrgtilknytningState> findByNavident(String navident) {
        return orgtilknytningerByNavident.getOrDefault(navident, Collections.emptyMap()).values();
    }

    @Override
    public List<OrgtilknytningState> findByNavidentIn(List<String> navidents) {
        return orgtilknytningerByNavident.entrySet().stream()
            .filter(e -> navidents.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .flatMap(identTilknytninger -> identTilknytninger.values().stream())
            .collect(Collectors.toList());
    }

    @Override
    public Collection<OrgtilknytningState> getAll() {
        return orgtilknytningByFid.values();
    }

    @Override
    public int count() {
        return orgtilknytningByFid.size();
    }

    @Override
    public void update(String fid, OrgtilknytningState newOrgtilknytningState) {
        if (newOrgtilknytningState == null) {
            removeFromState(fid);
        } else {
            addToState(newOrgtilknytningState);
        }
    }

    @Override
    public void clear() {
        orgtilknytningByFid.clear();
        orgtilknytningerByNavident.clear();
    }

    private void addToState(OrgtilknytningState newOrgtilknytningState) {
        orgtilknytningByFid.put(newOrgtilknytningState.fid(), newOrgtilknytningState);

        var navidentOrgtilknytninger = orgtilknytningerByNavident.get(newOrgtilknytningState.navident());
        if (navidentOrgtilknytninger == null) {
            navidentOrgtilknytninger = new ConcurrentHashMap<>();
            orgtilknytningerByNavident.put(newOrgtilknytningState.navident(), navidentOrgtilknytninger);
        }
        navidentOrgtilknytninger.put(newOrgtilknytningState.fid(), newOrgtilknytningState); // Sett inn ny orgtilknytning, eller bytt ut med ny state evt.
    }

    private void removeFromState(String fid) {
        var eksisterede = orgtilknytningByFid.get(fid);
        if (eksisterede != null) {
            var navidentOrgtilknytninger = orgtilknytningerByNavident.get(eksisterede.navident());
            if (navidentOrgtilknytninger != null) {
                navidentOrgtilknytninger.remove(fid);
            }
        }
        orgtilknytningByFid.remove(fid);
    }
}
