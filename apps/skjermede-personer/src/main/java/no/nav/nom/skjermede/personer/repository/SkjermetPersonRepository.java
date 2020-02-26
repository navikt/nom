package no.nav.nom.skjermede.personer.repository;

import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkjermetPersonRepository extends JpaRepository<SkjermetPerson, Long> {
    Optional<SkjermetPerson> save(SkjermetPerson skjermetPerson);
    Optional<SkjermetPerson> findAllByPersonident(String personident);

    @Query(value = "SELECT nextval('T_SKJERMET_PERSON_FUNK_SEQ')", nativeQuery = true)
    Long nextFunctionalId();
}
