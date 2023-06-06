package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;

import java.time.LocalDate;

public record OpprettOrgtilknytning (
    String navident,
    String orgenhetId,
    LocalDate gyldigFom,
    InputMetadata inputMetadata,
    OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData
) {}
