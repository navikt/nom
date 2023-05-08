package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;

import java.time.LocalDate;
import java.util.Optional;

public record EndrePrimaeOrgtilknytning(
    String navident,
    String orgEnhetId,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom,
    boolean primaer,
    InputMetadata inputMetadata,
    OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData
) {}
