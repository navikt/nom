package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;

import java.time.LocalDate;

public record AvvikleOrgtilknytning(
    String navident,
    String orgenhetId,
    LocalDate gyldigTom,
    String masseflyttetFraFid,
    InputMetadata inputMetadata,
    OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData
) implements OrgtilknytningCommand {
    public AvvikleOrgtilknytning(String navident, String orgenhetId, LocalDate gyldigTom, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData) {
        this(navident, orgenhetId, gyldigTom, null, inputMetadata, additionalAgressoData);
    }
}
