package no.nav.nom.apps.api.repository.hendelse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HendelseRepository extends JpaRepository<Hendelse, UUID> {

    List<Hendelse> findAllByFunksjonellId(UUID fid);

    List<Hendelse> findAllByDataType(Hendelse.DataType dataType);

    @Query(value =  "SELECT * FROM T_HENDELSER a " +
                    "INNER JOIN T_HENDELSER b ON a.FUNKSJONELL_ID = b.FUNKSJONELL_ID " +
                    "WHERE b.data ->> :jsonFieldKey = :jsonFieldValue",
            nativeQuery = true)
    List<Hendelse> findHendelseByJson(String jsonFieldKey, String jsonFieldValue);

    @Query(value =  "SELECT * FROM T_HENDELSER a " +
                    "INNER JOIN T_HENDELSER b ON a.FUNKSJONELL_ID = b.FUNKSJONELL_ID " +
                    "WHERE b.data ->> :jsonFieldKey IN (:jsonFieldValue)",
            nativeQuery = true)
    List<Hendelse> findHendelseByJsonBulk(String jsonFieldKey, List<String> jsonFieldValue);

    @Query(value = "SELECT * FROM T_HENDELSER WHERE lower(data ->> :jsonFieldKey) like :jsonFieldValue%",
            nativeQuery = true)
    List<Hendelse> findHendelserByJsonMatch(String jsonFieldKey, String jsonFieldValue);
}