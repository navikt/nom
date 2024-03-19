package no.nav.nom.apps.api.ressurs.api.v1.pdlfullmakt;

import no.nav.nom.contract.nom.Sektor;

public record AnsattPersonResponse(AnsattPerson ressurs) {
    public record AnsattPerson(
        String navident,
        String startdato,
        String sluttdato,

        Sektor sektor
    ) { }
}
