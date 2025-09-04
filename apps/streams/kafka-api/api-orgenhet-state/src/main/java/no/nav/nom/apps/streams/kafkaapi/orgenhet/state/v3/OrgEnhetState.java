package no.nav.nom.apps.streams.kafkaapi.orgenhet.state.v3;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

public record OrgEnhetState(
    String id,
    String navn,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    NomNivaa nomNivaa,
    OrgEnhetsType orgEnhetsType
) {}
