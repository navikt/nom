package no.nav.nom.apps.admin.sanitycheck.dto.orgenhet;

import java.time.LocalDate;

public record OrgEnhetOrganisering(
    String orgEnhetId,
    String uniqueAgressoId,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
}
