package no.nav.nom.apps.admin.sanitycheck.dto;

public record OrgEnhetStartOgSluttdato(
        int totaltAntallEnheter,
        int antallEnheterMedStartdato,
        int antallEnheterMedSluttdato,
        int antallEnheterMedSluttdatoFoerStartdato

) {
}
