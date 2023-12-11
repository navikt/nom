package no.nav.nom.apps.api.ressurs.eventsourcing.metadata;

import no.nav.nom.contract.nom.eventsource.Event;

import java.time.LocalDateTime;

public interface RessursMetadataRepository {

    void saveOpprettelseMetadata(String navident, Event.Metadata metadata);
    void saveEndringMetadata(String navident, Event.Metadata metadata, String type);
    String findOpprettetAvByNavident(String navident);
    LocalDateTime findOpprettetDatoByNavident(String navident);
    String findEndretAvByNavident(String navident);
    LocalDateTime findEndretDatoByNavident(String navident);
    String findEndringEventByNavident(String navident);
    int countOpprettelseMetadata();
}
