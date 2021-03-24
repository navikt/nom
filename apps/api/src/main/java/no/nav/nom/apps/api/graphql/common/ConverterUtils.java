package no.nav.nom.apps.api.graphql.common;

import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.apps.api.dvh.dto.DvhRessurs;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

public class ConverterUtils {

    public static Organisasjonsenhet convert(DvhOrganisasjonsenhet orgEnht) {
        return Organisasjonsenhet.builder()
                .agressoId(orgEnht.getAgressoId())
                .navn(orgEnht.getNavn())
                .gyldigFom(orgEnht.getGyldigFom())
                .gyldigTom(orgEnht.getGyldigTom())
                .build();
    }

    public static Ressurs convert(DvhRessurs dvhRessurs) {
        return Ressurs.builder()
                .navIdent(dvhRessurs.getNavIdent())
                .personIdent(dvhRessurs.getPersonIdent())
                .build();
    }
}
