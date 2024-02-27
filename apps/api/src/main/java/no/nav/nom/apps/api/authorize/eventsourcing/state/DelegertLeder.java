package no.nav.nom.apps.api.authorize.eventsourcing.state;

import java.time.LocalDate;

public record DelegertLeder(
    String fid,
    String til,
    String av,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {}
