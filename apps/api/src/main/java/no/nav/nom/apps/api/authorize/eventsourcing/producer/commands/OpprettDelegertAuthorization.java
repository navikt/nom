package no.nav.nom.apps.api.authorize.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OpprettDelegertAuthorization(
    String til,
    String av,
    String forOrgenhet,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    InputMetadata inputMetadata
) {}
