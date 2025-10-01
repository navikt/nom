package no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgEnhetLederEventSourceRepository extends JpaRepository<OrgEnhetLederEventSource, String> {
}
