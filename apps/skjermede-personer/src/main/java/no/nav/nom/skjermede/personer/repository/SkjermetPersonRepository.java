package no.nav.nom.skjermede.personer.repository;

import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkjermetPersonRepository extends JpaRepository<SkjermetPerson, Long> {
    List<SkjermetPerson> findAllByPersonident(String personident);
    SkjermetPerson findDistinctByPersonidentAndSkjermetTomNull(String personident);
    List<SkjermetPerson> findAllByTilknyttetRessurs(String tilknyttetRessurs);
    SkjermetPerson findDistinctByFunksjonellId(Long funksjonellId);
    SkjermetPerson findDistinctByFunksjonellIdAndSkjermetTomNotNull(Long funksjonellId);
    void deleteByFunksjonellId(Long funksjonellId);
    boolean existsByFunksjonellId(Long funksjonellId);
    boolean existsByPersonidentAndSkjermetTomNull(String personident);

    @Query(value = "SELECT nextval('T_SKJERMET_PERSON_FUNK_SEQ')", nativeQuery = true)
    Long nextFunctionalId();
}
