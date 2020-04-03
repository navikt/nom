package no.nav.nom.skjermede.personer.skjerming;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkjermetPersonRepository extends JpaRepository<SkjermetPerson, Long> {
    //Optional<SkjermetPerson> findByPersonident(String personident);

    List<SkjermetPerson> findAllByPersonidentAndTilknyttetRessurs(String personident, String tilknyttetRessurs);

    @Query(value = "SELECT * FROM T_SKJERMET_PERSON WHERE PERSONIDENT = :personident AND (SKJERMET_TOM IS NULL OR SKJERMET_TOM >= CURRENT_DATE)", nativeQuery = true)
    Optional<SkjermetPerson> getSkjermetPersonByPersonident(String personident);

    @Query(value =  "SELECT * FROM T_SKJERMET_PERSON " +
                    "WHERE TILKNYTTET_RESSURS = :tilknyttetRessurs " +
                    "AND (SKJERMET_TOM IS NULL OR CURRENT_DATE <= SKJERMET_TOM)", nativeQuery = true)
    List<SkjermetPerson> findAllByTilknyttetRessursAndValid(String tilknyttetRessurs);

    //Optional<SkjermetPerson> findDistinctByPersonidentAndSkjermetTomNull(String personident);
    List<SkjermetPerson> findAllByPersonident(String personident);
    List<SkjermetPerson> findAllByTilknyttetRessurs(String personident);

    boolean existsByPersonidentAndSkjermetTomNull(String personident);

    @Query(value =  "SELECT COUNT(DISTINCT(personident)) FROM T_SKJERMET_PERSON " +
                    "WHERE (skjermet_tom IS NULL OR skjermet_tom >= CURRENT_DATE) " +
                    "AND tilknyttet_ressurs IS NULL", nativeQuery = true)
    long countDistinctSkjermetAnsatte();

    @Query(value =  "SELECT COUNT(DISTINCT(personident)) FROM T_SKJERMET_PERSON " +
                    "WHERE (skjermet_tom IS NULL OR skjermet_tom >= CURRENT_DATE) " +
                    "AND tilknyttet_ressurs IS NOT NULL", nativeQuery = true)
    long countDistinctSkjermetFamilie();
}
