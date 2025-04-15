package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DelayedEventRepository extends JpaRepository<DelayedEvent, UUID> {

    List<DelayedEvent> findByDelayedUntilLessThanEqual(LocalDate localDate);

    @Modifying
    @Query(value = "DELETE FROM delayed_event WHERE event ->> 'fid' = :fid", nativeQuery = true)
    @Transactional
    void deleteDelayedOrgtilknytningEvents(String fid);
}
