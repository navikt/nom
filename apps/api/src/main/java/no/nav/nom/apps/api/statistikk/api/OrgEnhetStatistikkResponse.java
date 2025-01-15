package no.nav.nom.apps.api.statistikk.api;

public record OrgEnhetStatistikkResponse(
    RessursStatistikk ressursStatistikk,
    int antallLovNoder,
    int antallNavArbeidOgYtelser,
    int antallNavFamilieOgPensjonsytelser,
    int antallHjelpemidlerOgTilrettelegging,
    int antallKlageinstanser,
    int antallNavKontaktsenter,
    int antallKontrollKonrollenheter,
    int antallFylkekontor,
    int antallNavKontorer,
    int antallArbeidslivssenter,
    int antallArbeidsraadgivning,
    int antallTiltak
) {

}
