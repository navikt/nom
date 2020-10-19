package no.nav.nom.apps.nom.enheter.enhet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganiseringRepository extends JpaRepository<Organisering, Long> {

    @Query(value =  "SELECT * FROM T_ENHET " +
                    "WHERE dvh_id = :dvhId " +
                    "AND gyldig_fra <= CURRENT_TIMESTAMP " +
                    "AND (gyldig_til IS NULL OR gyldig_til > CURRENT_DATE)", nativeQuery = true)
    Organisering findByOverAndUnderEnhetAndValid(Long overEnhet, Long underEnhet);

}
