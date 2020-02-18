package no.nav.nom.apps.nom.ressurser.repository;

import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RessursRepository extends JpaRepository<Ressurs, Long> {
    Ressurs findBypersonident(String personIdent);
}

