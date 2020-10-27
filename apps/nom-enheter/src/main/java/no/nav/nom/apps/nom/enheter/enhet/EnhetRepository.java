package no.nav.nom.apps.nom.enheter.enhet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    @Query(value =  "SELECT * FROM T_ENHET e " +
                    "LEFT JOIN T_ORGANISERING o ON (e.ENHETSNR = o.UNDERENHET) " +
                    "WHERE o.UNDERENHET IS NULL", nativeQuery = true)
    Enhet findRotNode();

    @Query(value =  "SELECT * FROM T_ENHET " +
                    "WHERE ENHETSNR = :enhetsNr " +
                    "AND gyldig_fra <= CURRENT_DATE " +
                    "AND (gyldig_til IS NULL OR gyldig_til > CURRENT_DATE)", nativeQuery = true)
    Optional<Enhet> findByEnhetsnrAndValid(Long enhetsNr);
}
