package no.nav.nom.apps.api.orgtilknytning.contract;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface OrgtilknytningService {
    Collection<OrgtilknytningState> getAllOrgtilknytninger();

    Collection<OrgtilknytningState> getOrgtilknytningerByNavIdent(String navident);

    default Map<String, Collection<OrgtilknytningState>> getOrgtilknytningerByNavidenter(Set<String> navidenter) {
        // Use a NOOP-transformer to preserve original use.
        return getOrgtilknytningerByNavidenter(navidenter, Function.identity());
    }
    List<OrgtilknytningState> getOrgtilknytningerByEnhet(String id);

    <T> Map<String, Collection<T>> getOrgtilknytningerByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer);


    Collection<Orgtilknytning> getAllKoblingerOld();

    List<Orgtilknytning> getKoblingerByNavIdentOld(String navIdent);

    default Map<String, List<Orgtilknytning>> getKoblingerByNavidenterOld(Set<String> navidenter) {
        // Use a NOOP-transformer to preserve original use.
        return getKoblingerByNavidenterOld(navidenter, Function.identity());
    }

    <T> Map<String, List<T>> getKoblingerByNavidenterOld(Set<String> navidenter, Function<Orgtilknytning, T> transformer);

}
