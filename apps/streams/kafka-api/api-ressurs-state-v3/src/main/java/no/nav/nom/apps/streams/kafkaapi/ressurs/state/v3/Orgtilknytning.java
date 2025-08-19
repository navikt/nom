package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v3;

import java.time.LocalDate;

public record Orgtilknytning(
    String fid,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    boolean dagligOppfolging
) {}
