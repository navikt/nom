package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.Optional;

public record Orgtilknytning(
    String fid,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    boolean dagligOppfolging,
    Sektor sektor,
    Tilknytningtype tilknytningtype
) {}
