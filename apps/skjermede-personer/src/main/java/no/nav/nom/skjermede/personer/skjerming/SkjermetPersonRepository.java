package no.nav.nom.skjermede.personer.skjerming;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkjermetPersonRepository extends JpaRepository<SkjermetPerson, Long> {

    List<SkjermetPerson> findAllByPersonident(String personident);

    List<SkjermetPerson> findAllByKafkaProduced(boolean produced);

    @Query(value = "SELECT * FROM T_SKJERMET_PERSON " +
                   "WHERE TILKNYTTET_RESSURS = :tilknyttetRessurs AND " +
                   "(SKJERMET_TIL IS NULL OR SKJERMET_TIL >= current_timestamp) AND " +
                   "SKJERMET_FRA <= current_timestamp", nativeQuery = true)
    List<SkjermetPerson> findAllByTilknyttetRessurs(String tilknyttetRessurs);

    @Query(value = "SELECT * FROM T_SKJERMET_PERSON " +
                   "WHERE PERSONIDENT = :personident AND " +
                   "(SKJERMET_TIL IS NULL OR SKJERMET_TIL >= current_timestamp) AND " +
                   "SKJERMET_FRA <= current_timestamp", nativeQuery = true)
    Optional<SkjermetPerson> getSkjermetPersonByPersonident(String personident);

    @Query(value = "SELECT * FROM T_SKJERMET_PERSON " +
                   "WHERE ENDRING_SENDT_TPS IS NULL OR " +
                   "(SKJERMET_TIL IS NOT NULL AND OPPHOR_SENDT_TPS IS NULL) " +
                   "ORDER BY SKJERMET_FRA", nativeQuery = true)
    List<SkjermetPerson> getAllIkkeSendtTilTps();

    @Query(value =  "SELECT * FROM T_SKJERMET_PERSON " +
                    "WHERE TILKNYTTET_RESSURS = :tilknyttetRessurs " +
                    "AND (SKJERMET_TIL IS NULL OR current_timestamp <= SKJERMET_TIL) " +
                    "AND SKJERMET_FRA <= current_timestamp", nativeQuery = true)
    List<SkjermetPerson> findAllByTilknyttetRessursAndValid(String tilknyttetRessurs);

    @Query(value =  "SELECT COUNT(DISTINCT(personident)) FROM T_SKJERMET_PERSON " +
                    "WHERE (SKJERMET_TIL IS NULL OR SKJERMET_TIL >= current_timestamp) " +
                    "AND SKJERMET_FRA <= current_timestamp " +
                    "AND tilknyttet_ressurs IS NULL", nativeQuery = true)
    long countDistinctSkjermetAnsatte();

    @Query(value =  "SELECT COUNT(DISTINCT(personident)) FROM T_SKJERMET_PERSON " +
                    "WHERE (SKJERMET_TIL IS NULL OR SKJERMET_TIL >= current_timestamp) " +
                    "AND SKJERMET_FRA <= current_timestamp " +
                    "AND tilknyttet_ressurs IS NOT NULL", nativeQuery = true)
    long countDistinctSkjermetFamilie();
}
