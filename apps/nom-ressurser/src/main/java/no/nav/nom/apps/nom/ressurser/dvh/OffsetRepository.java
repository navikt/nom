package no.nav.nom.apps.nom.ressurser.dvh;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OffsetRepository extends JpaRepository<Offset, Long> {
    Offset findByOffsetName(String offsetName);
}
