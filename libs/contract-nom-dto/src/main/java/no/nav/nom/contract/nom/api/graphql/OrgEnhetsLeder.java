package no.nav.nom.contract.nom.api.graphql;

import java.time.LocalDate;

public record OrgEnhetsLeder(
    Ressurs ressurs,
    LocalDate gyldigFom,
    LocalDate gyldigTom
){}

