package no.nav.nom.apps.api.authorize.eventsourcing.recoverybackup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizeEventSourceRepository extends JpaRepository<AuthorizeEventSource, String> {}