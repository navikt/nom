package no.nav.nom.apps.api.orgtilknytning.contract;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface OrgtilknytningService {
    Collection<OrgtilknytningState> getAll();

    Collection<OrgtilknytningState> getByNavIdent(String navident);

    default Map<String, Collection<OrgtilknytningState>> getByNavidenter(Set<String> navidenter) {
        // Use a NOOP-transformer to preserve original use.
        return getByNavidenter(navidenter, Function.identity());
    }
    List<OrgtilknytningState> getByEnhet(String id);

    <T> Map<String, Collection<T>> getByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer);
}
