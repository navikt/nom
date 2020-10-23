package no.nav.nom.apps.nom.enheter.enhet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnhetRepository extends JpaRepository<Enhet, Long> {

    @Query(value =  "SELECT * FROM T_ENHET " +
                    "WHERE dvh_id = :dvhId " +
                    "AND gyldig_fra <= CURRENT_DATE " +
                    "AND (gyldig_til IS NULL OR gyldig_til > CURRENT_DATE)", nativeQuery = true)
    Enhet findByDvhIdAndValid(String dvhId);

    List<Enhet> findByDvhId(String dvhId);

    @Query(value = "SELECT nextval('T_ENHET_ENHETNR_SEQ')", nativeQuery = true)
    long getNextEnhetNr();
}
