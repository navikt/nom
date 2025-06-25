package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface DelayedEventReactiveRepository extends ReactiveCrudRepository<DelayedEventR2dbc, UUID> {
}
