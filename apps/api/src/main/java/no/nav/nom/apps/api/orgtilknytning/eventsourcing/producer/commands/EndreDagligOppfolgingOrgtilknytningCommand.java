package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record EndreDagligOppfolgingOrgtilknytningCommand(
    String fid,
    String navident,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    boolean erDagligOppfolging,
    String masseflyttetFraFid,
    InputMetadata inputMetadata,
    OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData
) implements OrgtilknytningCommand {
    public EndreDagligOppfolgingOrgtilknytningCommand(String fid, String navident, String orgenhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean erDagligOppfolging, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData) {
        this(fid, navident, orgenhetId, gyldigFom, gyldigTom, erDagligOppfolging, null, inputMetadata, additionalAgressoData);
    }
}
