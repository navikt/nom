package no.nav.nom.apps.nom.ressurser.repository;

import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RessursRepository extends JpaRepository<Ressurs, Long> {
    Optional<Ressurs> findByPersonident(String personIdent);

    @Query(value = "SELECT nextval('T_RESSURS_FUNK_SEQ')", nativeQuery = true)
    Long nextFunctionalId();
}

