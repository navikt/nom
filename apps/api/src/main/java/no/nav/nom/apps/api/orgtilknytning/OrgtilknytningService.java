package no.nav.nom.apps.api.orgtilknytning;

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

import static no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository.UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE;

@Service
public class OrgtilknytningService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public OrgtilknytningService(OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public Collection<OrgtilknytningState> getAll() {
        return orgtilknytningStateRepository.getAll();
    }

    public Collection<OrgtilknytningState> getByNavIdent(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident);
    }

    public Collection<OrgtilknytningState> getByNavIdent(String navident, boolean hentForEksterneUavhengigAvFeatureFlag) {
        // TODO: remove når feature flag ikke brukes mer
        String flag = UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE; // Kodelinje lagt inn  for å minne på fjerning av metoden når feature toggle fjernes
        return orgtilknytningStateRepository.findByNavident(navident, hentForEksterneUavhengigAvFeatureFlag);
    }

    public List<OrgtilknytningState> getByEnhet(String id) {
        return getAll().stream()
            .filter(ot -> ot.orgEnhetId().equals(id))
            .toList();
    }

    public <T> Map<String, Collection<T>> getByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer) {
        return navidenter.stream().collect(Collectors.toMap(navident -> navident, navident -> {
            var value = this.getByNavIdent(navident);
            return value == null ? Collections.emptyList() : value.stream().map(transformer).toList();
        }));
    }
}
