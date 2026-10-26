package no.nav.nom.apps.sfintegrasjon.orgenhet.domain;

import lombok.Builder;
import no.nav.nom.contract.nom.NomNivaa;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OverOrgEnhet(
    String id,
    NomNivaa nivaa,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
}
