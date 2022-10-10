package no.nav.nom.apps.api.hendelse.contract;

import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HendelseRepository extends GenericStorage {

    @Query(
            value = """
                    SELECT * FROM T_GENERIC_STORAGE
                    WHERE uuid(document ->> 'fid') = :fid
                    """,
            nativeQuery = true
    )
    List<GenericStorageElement> findAllByFunksjonellId(UUID fid);

    @Query(
            value = """
                    SELECT * FROM T_GENERIC_STORAGE
                    WHERE document @> cast('{"hendelseDataType": "'||:hendelseDataType||'"}' AS jsonb)
                    """
            , nativeQuery = true)
    List<GenericStorageElement> findAllByDataType(String hendelseDataType);

    @Query(
            value = """
                    SELECT * FROM T_GENERIC_STORAGE a
                    INNER JOIN T_GENERIC_STORAGE b ON a.document ->> 'fid' = b.document ->> 'fid'
                    WHERE b.document -> 'hendelseData' ->> :jsonFieldKey = :jsonFieldValue
                    """,
            nativeQuery = true)
    List<GenericStorageElement> findHendelseByJson(String jsonFieldKey, String jsonFieldValue);

    @Query(
            value = """
                    SELECT * FROM T_GENERIC_STORAGE a
                    INNER JOIN T_GENERIC_STORAGE b ON a.document ->> 'fid' = b.document ->> 'fid'
                    WHERE b.document -> 'hendelseData' ->> :jsonFieldKey IN (:jsonFieldValue)
                    """,
            nativeQuery = true
    )
    List<GenericStorageElement> findHendelseByJsonBulk(String jsonFieldKey, List<String> jsonFieldValue);

    @Query(
            value = """
                    SELECT a.* FROM T_GENERIC_STORAGE a
                    WHERE a.DOCUMENT ->> 'fid' in (
                       SELECT b.document ->> 'fid' FROM T_GENERIC_STORAGE b
                       WHERE lower(b.document -> 'hendelseData' ->> :jsonFieldKey) like '%'||:jsonFieldValue||'%'
                       AND DOCUMENT @> cast('{"hendelseDataType": "'||:hendelseDataType||'"}' AS jsonb)
                    )
                    """
            , nativeQuery = true)
    List<GenericStorageElement> findHendelserByJsonMatch(String jsonFieldKey, String jsonFieldValue, String hendelseDataType);

}
