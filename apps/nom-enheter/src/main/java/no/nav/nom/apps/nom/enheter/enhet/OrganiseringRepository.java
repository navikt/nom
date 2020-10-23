package no.nav.nom.apps.nom.enheter.enhet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganiseringRepository extends JpaRepository<Organisering, Long> {

    @Query(value =  "SELECT * FROM T_ORGANISERING " +
                    "WHERE OVERENHET = :overEnhet AND UNDERENHET = :underEnhet " +
                    "AND GYLDIG_FRA <= CURRENT_DATE " +
                    "AND (GYLDIG_TIL IS NULL OR GYLDIG_TIL > CURRENT_DATE)", nativeQuery = true)
    Organisering findByOverAndUnderEnhetAndValid(Long overEnhet, Long underEnhet);

}
