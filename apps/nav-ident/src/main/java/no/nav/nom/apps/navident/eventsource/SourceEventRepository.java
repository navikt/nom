package no.nav.nom.apps.navident.eventsource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourceEventRepository extends JpaRepository<SourceEvent, Long> {

    Optional<SourceEvent> findFirstByOrderByIdDesc();
}
