package no.nav.nom.libs.state.ressurs;

import no.nav.nom.contract.nom.state.RessursState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface RessursStateRepository {
    Optional<RessursState> findByFid(String fid);

    Optional<RessursState> findByPersonident(String personident);

    Optional<RessursState> findByNavident(String navident);

    <T> Optional<T> findByNavident(String navIdent, Function<RessursState, T> withMapper);

    Collection<RessursState> getAll();

    Collection<RessursState> getAllBareEksterne();

    Collection<RessursState> getAllByFid();

    int count();

    Map<String, RessursState> findByPersonidentBulk(List<String> personIdenter);

    Map<String, RessursState> findByNavidentBulk(List<String> navIdenter);

    void update(RessursState ressursEventData);

    void clear();

    void remove(String fid);
}
