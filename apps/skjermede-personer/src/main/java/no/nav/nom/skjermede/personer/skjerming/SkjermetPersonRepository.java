package no.nav.nom.skjermede.personer.skjerming;

import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkjermetPersonRepository extends JpaRepository<SkjermetPerson, Long> {
    Optional<SkjermetPerson> findAllByPersonident(String personident);
    Optional<SkjermetPerson> findDistinctByFunksjonellId(Long funksjonellId);
    Optional<SkjermetPerson> findDistinctByFunksjonellIdAndSkjermetTomNull(Long funksjonellId);
    List<SkjermetPerson> findAllByTilknyttetRessurs(String personident);
    boolean existsByPersonidentAndSkjermetTomNull(String personident);

    @Query(value = "SELECT nextval('T_SKJERMET_PERSON_FUNK_SEQ')", nativeQuery = true)
    Long nextFunctionalId();
}
