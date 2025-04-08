package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
public record OrgtilknytningEventExt(
    OrgtilknytningEvent orgtilknytningEvent,
    MessageMetadata messageMetadata
){}
