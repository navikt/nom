package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DelayedEventRepository extends JpaRepository<DelayedEvent, UUID> {

    List<DelayedEvent> findByDelayedUntilBefore(LocalDate localDate);
}
