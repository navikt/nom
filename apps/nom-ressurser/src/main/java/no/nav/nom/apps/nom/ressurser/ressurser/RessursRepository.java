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

    List<Ressurs> findAllByPersonident(String personIdent);

    List<Ressurs> findAllByProduced(boolean produced);

    List<Ressurs> findByNavIdent(String navIdent);

    @Query(value = "SELECT nextval('T_RESSURS_FUNK_SEQ')", nativeQuery = true)
    Long nextFunctionalId();

    @Query(value = "SELECT COUNT(DISTINCT funksjonell_id) FROM T_RESSURS WHERE (sluttdato IS NULL OR sluttdato <= CURRENT_DATE) AND ressurstype = :ressurstype", nativeQuery = true)
    long countDistinctByFunksjonellIdAndValid(@Param("ressurstype") String ressurstype);
}

