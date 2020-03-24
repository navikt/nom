package no.nav.nom.apps.nom.ressurser.ressurser;

import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RessursRepository extends JpaRepository<Ressurs, Long> {
    Optional<Ressurs> findByPersonident(String personIdent);

    List<Ressurs> findAllByPersonidentAndRessurstype(String personIdent, Ressurstype ressurstype);

    List<Ressurs> findAllByProduced(boolean produced);

    List<Ressurs> findByNavident(String navident);

    Optional<Ressurs> findFirstByNavidentAndEpostNotNullOrderByOpprettetDatoDesc(String navident);

    @Query(value = "SELECT COUNT(DISTINCT navident) FROM T_RESSURS WHERE (sluttdato IS NULL OR sluttdato <= CURRENT_DATE) AND ressurstype = :ressurstype", nativeQuery = true)
    long countDistinctByFunksjonellIdAndValid(@Param("ressurstype") String ressurstype);
}

