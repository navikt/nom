package no.nav.nom.apps.streams.kafkaapi.orgenhet.state.v2;

import no.nav.nom.contract.nom.OrgEnhetsKategori;

import java.time.LocalDate;
import java.util.Optional;

public record OrgEnhetState(
    String id,
    String navn,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom,
    Optional<OrgEnhetsKategori> orgEnhetsKategori
) {}
