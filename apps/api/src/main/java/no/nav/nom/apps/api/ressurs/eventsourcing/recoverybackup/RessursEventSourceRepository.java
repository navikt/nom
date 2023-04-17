package no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

@Repository
public interface RessursEventSourceRepository extends JpaRepository<RessursEventSource, String> {
    Stream<RessursEventSource> findAllBy();
}