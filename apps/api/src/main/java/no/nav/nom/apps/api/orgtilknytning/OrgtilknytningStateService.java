package no.nav.nom.apps.api.orgtilknytning;

import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrgtilknytningStateService implements OrgtilknytningService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public OrgtilknytningStateService(OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    @Override
    public Collection<OrgtilknytningState> getAllOrgtilknytninger() {
        return orgtilknytningStateRepository.getAll();
    }

    @Override
    public Collection<OrgtilknytningState> getOrgtilknytningerByNavIdent(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident);
    }

    @Override
    public List<OrgtilknytningState> getOrgtilknytningerByEnhet(String id) {
        return getAllOrgtilknytninger().stream()
            .filter(ot -> ot.orgEnhetId().equals(id))
            .toList();
    }

    @Override
    public <T> Map<String, Collection<T>> getOrgtilknytningerByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer) {
        return navidenter.stream().collect(Collectors.toMap(navident -> navident, navident -> {
            var value = this.getOrgtilknytningerByNavIdent(navident);
            return value == null ? Collections.emptyList() : value.stream().map(transformer).toList();
        }));
    }
}
