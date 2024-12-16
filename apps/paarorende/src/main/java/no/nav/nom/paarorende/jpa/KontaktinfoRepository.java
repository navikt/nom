package no.nav.nom.paarorende.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface KontaktinfoRepository extends JpaRepository<Kontaktinfo, Long> {

    List<Kontaktinfo> findAllByNavidentAndDatoSlettetIsNull(String navident);

    Optional<Kontaktinfo> findDistinctByKontakId(String kontaktId);

    @Query(
        value = "SELECT COUNT(*) FROM KONTAKTINFO WHERE NAVIDENT = :navident AND DATO_SLETTET IS NULL",
        nativeQuery = true
    )
    int countActiveByNavident(String navident);

    @Modifying
    @Transactional
    @Query(
        value = "DELETE FROM KONTAKTINFO WHERE DATE(DATO_SLETTET + INTERVAL '2 month') < CURRENT_DATE",
        nativeQuery = true
    )
    int deleteOldKontaktInfo();
}
