package no.nav.nom.apps.api.graphql.common;

import no.nav.nom.apps.api.dvh.DvhOrganisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;

public class ConverterUtils {

    public static Organisasjonsenhet convertOrgEnhet(DvhOrganisasjonsenhet orgEnht){
        return Organisasjonsenhet.builder()
                .id(orgEnht.getNummer())
                .navn(orgEnht.getNavn())
                .gyldigFom(orgEnht.getGyldigFom())
                .gyldigTom(orgEnht.getGyldigTom())
                .build();
    }
}
