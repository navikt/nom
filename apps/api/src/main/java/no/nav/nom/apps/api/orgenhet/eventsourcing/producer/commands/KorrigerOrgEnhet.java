package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.OrgEnhetsKategori;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Optional;

public record KorrigerOrgEnhet(
        String id,
        String navn,
        LocalDate gyldigFom,
        Optional<LocalDate> gyldigTom,
        Optional<String> overordnetOrgEnhetFid,
        Optional<String> lederRessursFid,
        OrgEnhetsKategori orgEnhetsKategori,
        InputMetadata inputMetadata
) {}
