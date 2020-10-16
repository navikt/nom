package no.nav.nom.apps.nom.enheter.enhet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnhetRepository extends JpaRepository<Enhet, Long> {

    Enhet findByDvhIdAndGyldigTilIsNull(String dvhId);

    List<Enhet> findByDvhId(String dvhId);

    @Query(value = "SELECT nextval('SEQUENCE T_ENHETNR_SEQ START')", nativeQuery = true)
    long getNextEnhetNr();
}
