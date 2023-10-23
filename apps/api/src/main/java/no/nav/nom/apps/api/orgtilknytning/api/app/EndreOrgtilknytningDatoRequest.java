package no.nav.nom.apps.api.orgtilknytning.api.app;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;

@Builder
public record EndreOrgtilknytningDatoRequest(
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom
) {}

