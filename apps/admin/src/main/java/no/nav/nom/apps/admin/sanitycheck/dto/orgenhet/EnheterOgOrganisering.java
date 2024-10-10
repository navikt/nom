package no.nav.nom.apps.admin.sanitycheck.dto.orgenhet;

public record EnheterOgOrganisering(
    int aktiveEnheter,
    int aktiveOrganiseringer,
    int antallOverEnheter,
    int antallUnderEnheter,
    int aktiveEnheterUtenOverEnheter,
    int aktiveEnheterUtenUnderEnheter,
    int aktiveEnheterMedUnderEnheterUtenOverEnheter,
    int aktiveEnheterUtenNoenOrganisering
) {
}
