package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Optional;

public record OpprettOrgEnhet(
        String id,
        String navn,
        LocalDate gyldigFom,
        Optional<String> parentOrgEnhetFid,
        Optional<String> lederRessursFid,
        InputMetadata inputMetadata
) {}
