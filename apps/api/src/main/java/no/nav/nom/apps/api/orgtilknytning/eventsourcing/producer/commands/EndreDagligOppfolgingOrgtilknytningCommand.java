package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record EndreDagligOppfolgingOrgtilknytningCommand(
    String fid,
    String navident,
    String orgenhetId,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom,
    boolean erDagligOppfolging,
    InputMetadata inputMetadata,
    OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData
) {}
