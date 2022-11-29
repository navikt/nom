package no.nav.nom.apps.api.ressurs;

import no.nav.nom.contract.nom.state.RessursState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface IRessursService {
    Optional<RessursState> findByFid(UUID fid);
    Optional<RessursState> findByPersonident(String personident);
    <T> Optional<T> findByNavident(String navident, Function<RessursState, T> converter);
    default Optional<RessursState> findByNavident(String navident){
        return findByNavident(navident, Function.identity());
    }
    Collection<RessursState> getAll();
    List<RessursState> matchByNavident(String navident);

    Map<String, RessursState> findByPersonidentBulk(List<String> personIdenter);
    Map<String, RessursState> findByNavidentBulk(List<String> navIdenter);

    void clear();
}
