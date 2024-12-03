package no.nav.nom.paarorende.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KontaktinfoRepository extends JpaRepository<Kontaktinfo, Long> {

    List<Kontaktinfo> findAllByNavident(String navident);
    Optional<Kontaktinfo> findDistinctByKontakId(String kontaktId);
}
