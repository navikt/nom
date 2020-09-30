package no.nav.nom.apps.nom.ressurser.ressurser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RessursRepository extends JpaRepository<Ressurs, Long> {
    Optional<Ressurs> findByPersonident(String personIdent);

    Optional<Ressurs> findByNavident(String navident);

    List<Ressurs> findAllByProduced(boolean produced);

    @Query(value = "SELECT COUNT(*) FROM T_RESSURS WHERE (sluttdato > CURRENT_DATE OR sluttdato IS NULL) AND ressurstype = :ressurstype", nativeQuery = true)
    long countActiveByRessurstype(@Param("ressurstype") String ressurstype);

    @Query(value = "SELECT COUNT(*) FROM T_RESSURS WHERE sluttdato <= CURRENT_DATE AND ressurstype = :ressurstype", nativeQuery = true)
    long countInactiveByRessurstype(@Param("ressurstype") String ressurstype);

    @Query(value = "SELECT * FROM T_RESSURS WHERE NAVIDENT = :navident AND (SLUTTDATO IS NULL OR SLUTTDATO > CURRENT_DATE)", nativeQuery = true)
    Optional<Ressurs> getGyldigRessursByNavident(String navident);

    @Query(value = "SELECT * FROM T_RESSURS WHERE PERSONIDENT = :personident AND (SLUTTDATO IS NULL OR SLUTTDATO > CURRENT_DATE)", nativeQuery = true)
    Optional<Ressurs> getGyldigRessursByPersonident(String personident);
}

