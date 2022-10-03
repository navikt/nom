package no.nav.nom.apps.api.orgenhet.implementations.postgresql;

import no.nav.nom.apps.api.hendelse.contract.HendelseRepository;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface OrgenhetHendelseRepository extends HendelseRepository {

    @Query(value = "SELECT * FROM T_GENERIC_STORAGE " +
            "WHERE DOCUMENT ->> 'fid' IN ( " +
            "   SELECT DOCUMENT ->> 'fid' FROM T_GENERIC_STORAGE " +
            "   WHERE DOCUMENT -> 'hendelseData' @> cast('{\"agressoId\": \"'||:agressoId||'\", \"navn\": \"'||:name||'\", \"agressoOrgNiv\": \"'||:orgniv||'\"}' AS jsonb)" +
            ")"
    , nativeQuery = true)
    List<GenericStorageElement> findEnhetByAgressoIdNivaAndName(String agressoId, String orgniv, String name);

    @Query(value = "SELECT * FROM T_GENERIC_STORAGE " +
            "WHERE DOCUMENT ->> 'fid' in ( " +
            "   SELECT document ->> 'fid' FROM T_GENERIC_STORAGE as b" +
            "   WHERE DOCUMENT -> 'hendelseData' @> cast('{\"agressoId\": \"'||:agressoId||'\", \"agressoOrgNiv\": \"'||:orgniv||'\"}' AS jsonb)" +
            ")"
            , nativeQuery = true)
    List<GenericStorageElement> findEnhetByAgressoIdNiva(String agressoId, String orgniv);


    @Query(value = "SELECT * FROM T_GENERIC_STORAGE " +
            "WHERE DOCUMENT ->> 'fid' in ( " +
            "   SELECT document ->> 'fid' FROM T_GENERIC_STORAGE as b" +
            "   WHERE DOCUMENT -> 'hendelseData' ->> 'agressoId' IN :agressoId" +
            ")"
            , nativeQuery = true)
    List<GenericStorageElement> findByAgressoIdIn(Set<String> agressoId);
}