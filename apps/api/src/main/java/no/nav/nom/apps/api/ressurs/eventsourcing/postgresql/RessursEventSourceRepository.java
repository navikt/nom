package no.nav.nom.apps.api.ressurs.eventsourcing.postgresql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RessursEventSourceRepository extends JpaRepository<RessursEventSource, String> {
}