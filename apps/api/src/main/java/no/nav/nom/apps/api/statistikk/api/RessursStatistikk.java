package no.nav.nom.apps.api.statistikk.api;

import lombok.Builder;

@Builder(toBuilder = true)
public record RessursStatistikk(
    long antallAktiveRessurser,
    long antallStatligeRessurser,
    long antallKommunaleRessurser,
    long antallEksterneRessurser
) {
}
