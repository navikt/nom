package no.nav.nom.apps.sfintegrasjon.outbox.repository;

import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SfOutboxRepository extends JpaRepository<SfOutbox, Long> {

    List<SfOutbox> getAllBySfStatusIn(Collection<SfStatus> sfStatuses);

    List<SfOutbox> getAllByOrgEnhetId(String orgEnhetId);

    List<SfOutbox> getAllBySfStatus(SfStatus sfStatus);
}
