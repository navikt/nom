package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

import no.nav.nom.contract.nom.Sektor;

public record RessursMedInaktivEnhet(
    String navident,
    String ressursFid,
    Sektor sektor,
    String orgEnhetId,
    String orgEnhetNavn,
    String uniqueAgressoId
) {
}
