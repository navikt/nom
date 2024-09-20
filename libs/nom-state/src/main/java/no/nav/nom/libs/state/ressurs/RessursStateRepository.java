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

    Map<String, RessursState> findByPersonidentBulk(List<String> personIdenter);

    Map<String, RessursState> findByNavidentBulk(List<String> navIdenter);

    Map<String, RessursState> findAllWithSluttdatoBeforeToday();

    Map<String, RessursState> findAllActiveRessurser();

    void update(RessursState ressursEventData);

    void clear();

    void remove(String fid);

    long count();

    long countEksterne();

    long countKommunale();

    long countStatlige();

    long countIkkeAvsluttede();

    long countIkkeAvsluttedeEksterne();

    long countIkkeAvsluttedeKommunale();

    long countIkkeAvsluttedeStatlige();

    long countFremtidige();

    long countFremtidigeEksterne();

    long countFremtidigeKommunale();

    long countFremtidigeStatlige();
}
