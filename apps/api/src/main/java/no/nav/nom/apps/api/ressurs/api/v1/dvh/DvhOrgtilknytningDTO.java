package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import java.time.LocalDate;

public record DvhOrgtilknytningDTO(
    String orgenhetId,
    LocalDate startdato,
    LocalDate sluttdato,
    String sektor,
    boolean erDagligOppfolging,
    String orgtilknytningType,
    String orgtilknytningFid
) {}
