package no.nav.nom.apps.api.statistikk.api;

public record GlobalStatistikkResponse(
    RessursStatistikk ressursStatistikk,
    int antallNavArbeidOgYtelser,
    int antallLovNoder,
    int antallNavFamilieOgPensjonsytelser,
    int antallHjelpemidlerOgTilrettelegging,
    int antallKlageinstanser,
    int antallNavKontaktsenter,
    int antallKontrollKonrollenheter,
    int antallFylkekontor,
    int antallNavKontorer
) {
}
