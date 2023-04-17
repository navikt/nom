package no.nav.nom.apps.api.orgTilknytning.eventsourcing.recoverybackup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgtilknytningEventSourceRepository extends JpaRepository<OrgtilknytningEventSource, String> {}