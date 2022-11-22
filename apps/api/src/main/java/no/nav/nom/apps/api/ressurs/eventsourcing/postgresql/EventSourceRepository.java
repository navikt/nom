package no.nav.nom.apps.api.ressurs.eventsourcing.postgresql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventSourceRepository extends JpaRepository<EventSource, String> {
    @Query("SELECT min(eventKey) FROM EventSource")
    Optional<String> findFirstSavedSourceEvent();
}