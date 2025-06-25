package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DelayedEventRepository extends JpaRepository<DelayedEventJpa, UUID> {

    List<DelayedEventJpa> findByDelayedUntilLessThanEqual(LocalDate localDate);

    @Modifying
    @Query(value = "DELETE FROM delayed_event WHERE event ->> 'fid' = :fid", nativeQuery = true)
    @Transactional
    void deleteDelayedOrgtilknytningEvents(String fid);
}
