package no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganiseringEventSourceRepository extends JpaRepository<OrganiseringEventSource, String> {}