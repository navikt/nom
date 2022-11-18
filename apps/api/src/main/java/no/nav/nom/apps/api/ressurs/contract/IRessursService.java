package no.nav.nom.apps.api.ressurs.contract;

import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    Map<String, TYPE> findByPersonidentBulk(List<String> personIdenter);
    Map<String, TYPE> findByNavidentBulk(List<String> navIdenter);
    IRessurs opprettRessurs(String personerident, Map<String, Object> data, HendelseMetadataSerialize metadata);
}
