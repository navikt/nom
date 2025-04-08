package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.nom.contract.nom.eventsource.RessursEvent;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
public record RessursEventExt(
    RessursEvent ressursEvent,
    MessageMetadata messageMetadata
){}
