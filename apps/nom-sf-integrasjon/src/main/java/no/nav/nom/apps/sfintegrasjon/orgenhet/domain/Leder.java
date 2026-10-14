package no.nav.nom.apps.sfintegrasjon.orgenhet.domain;

import java.time.LocalDate;

public record Leder(
    String navident,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {

}
