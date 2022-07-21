package no.nav.nom.apps.navident.eventsource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceEventRepository extends JpaRepository<SourceEvent, Long> {

}
