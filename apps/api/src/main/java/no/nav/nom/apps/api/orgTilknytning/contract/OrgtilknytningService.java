package no.nav.nom.apps.api.orgTilknytning.contract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface OrgtilknytningService {
    Collection<Orgtilknytning> getAllKoblinger();

    List<Orgtilknytning> getKoblingerByNavIdent(String navIdent);

    default Map<String, List<Orgtilknytning>> getKoblingerByNavidenter(Set<String> navidenter) {
        // Use a NOOP-transformer to preserve original use.
        return getKoblingerByNavidenter(navidenter, Function.identity());
    }

    <T> Map<String, List<T>> getKoblingerByNavidenter(Set<String> navidenter, Function<Orgtilknytning, T> transformer);
}
