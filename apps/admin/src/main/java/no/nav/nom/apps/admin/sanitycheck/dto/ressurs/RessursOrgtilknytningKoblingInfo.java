package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

public record RessursOrgtilknytningKoblingInfo(
    int totaltAntallRessurser,
    int antallAktiveOrgtilknytninger,
    int ressurserMedAktiveOrgtilknytninger,
    int ressurserUtenAktiveOrgtilknytninger,
    int ressurserMedAktiveOrgtilknytningerForEnhetSomErInaktivEllerUtenOrganisering,
    int ressursMedTilknytningerUtoverEnhetGyldigPeriode
) {
}
