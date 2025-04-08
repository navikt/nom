package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AggregateStateRepository extends JpaRepository<AggregateState, UUID> {

    List<AggregateState> findByNavident(String navident);

    List<AggregateState> findByDelayedUntilLessThanEqualAndMasseflyttingIsFalse(LocalDate delayedUntil);

    List<AggregateState> findAllByMasseflyttingIsTrue();
}
