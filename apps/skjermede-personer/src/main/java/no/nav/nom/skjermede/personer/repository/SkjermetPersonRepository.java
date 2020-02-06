package no.nav.nom.skjermede.personer.repository;

import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkjermetPersonRepository extends JpaRepository<SkjermetPerson, String> {
}
