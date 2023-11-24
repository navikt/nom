package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import java.time.LocalDate;

public record DvhOrgenhetDTO(
    String id,
    String navn,
    String type,
    String nivaa,
    String overodnetOrgenhetId,
    LocalDate gyldigFom,
    LocalDate GyldigTom,
    String agressoId,
    String agressoNivaa,
    String remedyEnhet
) {}
