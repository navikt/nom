package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2;

import java.time.LocalDate;
import java.util.Optional;

public record Orgtilknytning(
    String fid,
    String orgenhetId,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom,
    boolean dagligOppfolging
) {}
