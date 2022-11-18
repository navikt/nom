package no.nav.nom.apps.api.postgresql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSourceRepository extends JpaRepository<EventSource, String> {
    @Query("SELECT min(eventKey) FROM EventSource")
    Optional<String> findFirstSavedSourceEvent();
}