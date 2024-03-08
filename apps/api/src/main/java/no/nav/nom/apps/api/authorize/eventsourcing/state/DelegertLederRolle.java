package no.nav.nom.apps.api.authorize.eventsourcing.state;

import no.nav.nom.apps.api.common.DateUtils;

import java.time.LocalDate;

public record DelegertLederRolle(
    String fid,
    String til,
    String av,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
    public boolean isActive() {
        return DateUtils.isPeriodActive(gyldigFom, gyldigTom);
    }
}
