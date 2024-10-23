package no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning;

public record OrgtilknytningMedOrgEnhet(
    String navident,
    String uniqueAgressoId,
    String orgEnhetId,
    String gyldigFom,
    String gyldigTom,
    String orgEnhetGyldigFom,
    String orgEnhetGyldigTom
) {
}
