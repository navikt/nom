package no.nav.nom.apps.api.orgtilknytning.eventsourcing.state;

import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class OrgtilknytningStateRepository {
    private final Map<String, OrgtilknytningState> orgtilknytningByFid = new ConcurrentHashMap<>();
    private final Map<String, List<OrgtilknytningState>> orgtilknytningerByNavident = new ConcurrentHashMap<>();

    public Optional<OrgtilknytningState> findByFid(String fid) {
        return Optional.ofNullable(orgtilknytningByFid.get(fid));
    }

    public List<OrgtilknytningState> findByFidIn(List<String> fids) {
        return orgtilknytningByFid.entrySet().stream()
            .filter( e -> fids.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public List<OrgtilknytningState> findByNavident(String navident) {
        return orgtilknytningerByNavident.get(navident);
    }

    public List<OrgtilknytningState> findByNavidentIn(List<String> navidents) {
        return orgtilknytningerByNavident.entrySet().stream()
            .filter( e -> navidents.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .flatMap(Collection::stream)
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

        var navidentOrgtilknytninger = orgtilknytningerByNavident.get(newOrgtilknytningState.navident());
        if (navidentOrgtilknytninger == null) {
            navidentOrgtilknytninger = new CopyOnWriteArrayList<>(); // Det er kostbart med en CopyOnWrite, men per navIdent er dette en sjelden operasjon
        }
        navidentOrgtilknytninger.add(newOrgtilknytningState);
    }

    public void clear() {
        orgtilknytningByFid.clear();
        orgtilknytningerByNavident.clear();
    }

}
