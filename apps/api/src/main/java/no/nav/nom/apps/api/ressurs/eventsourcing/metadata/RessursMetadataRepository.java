package no.nav.nom.apps.api.ressurs.eventsourcing.metadata;

import no.nav.nom.contract.nom.eventsource.Event;

import java.time.LocalDateTime;

public interface RessursMetadataRepository {

    void save(String id, Event.Metadata metadata);

    String findOpprettetAvById(String id);
    LocalDateTime findOpprettetDatoById(String id);

    int count();
}
