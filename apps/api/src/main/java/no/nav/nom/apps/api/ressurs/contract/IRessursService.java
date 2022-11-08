package no.nav.nom.apps.api.ressurs.contract;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface IRessursService<TYPE> {
    Optional<TYPE> findByFid(UUID fid);
    Optional<TYPE> findByPersonident(String personident);
    <T> Optional<T> findByNavident(String navident, Function<TYPE, T> converter);
    default Optional<TYPE> findByNavident(String navident){
        return findByNavident(navident, Function.identity());
    }
    Collection<TYPE> getAll();
    List<TYPE> matchByNavident(String navident);
}
