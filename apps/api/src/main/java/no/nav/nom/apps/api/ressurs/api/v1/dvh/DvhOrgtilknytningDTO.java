package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import java.time.LocalDate;
import java.util.Optional;

public record DvhOrgtilknytningDTO(
    String orgenhetId,
    LocalDate startdato,
    Optional<LocalDate> sluttdato,
    boolean erDagligOppfolging
) {}
