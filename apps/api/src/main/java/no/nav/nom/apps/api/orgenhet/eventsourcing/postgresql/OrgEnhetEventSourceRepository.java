package no.nav.nom.apps.api.orgenhet.eventsourcing.postgresql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgEnhetEventSourceRepository extends JpaRepository<OrgEnhetEventSource, String> {}