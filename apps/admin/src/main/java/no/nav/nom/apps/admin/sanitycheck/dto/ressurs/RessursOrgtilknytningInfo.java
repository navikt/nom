package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

public record RessursOrgtilknytningInfo(
    int totaltAntallRessurser,
    int antallAktiveOrgtilknytninger,
    int ressurserMedAktiveOrgtilknytninger,
    int ressurserUtenAktiveOrgtilknytninger,
    int ressurserMedAktiveOrgtilknytningerForEnhetSomErInaktivEllerUtenOrganisering,
    int ressursMedTilknytningerUtoverEnhetGyldigPeriode
) {
}
