package no.nav.nom.skjermede.personer.repository;

import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SkjermetPersonRepository extends JpaRepository<SkjermetPerson, Long> {
    Optional<SkjermetPerson> findByPersonident(String personIdent);
    @Query(value = "SELECT nextval('T_SKJERMET_PERSON_FUNK_SEQ')", nativeQuery = true)
    Long nextFunctionalId();
}
