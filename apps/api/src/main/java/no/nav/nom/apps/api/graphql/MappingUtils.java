package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.RessursState;

import static java.util.Collections.singletonList;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.loggedInUserHasAccessScope;

public class MappingUtils {

    public static Ressurs toRessurs(RessursState ressursState) {
        final var sektor = singletonList(ressursState.sektor());
        final var personIdentView = shouldObfuscatePersonIdent() ? obfuscateFnr(ressursState.personident()) : ressursState.personident();

        return Ressurs.builder()
                .navident(ressursState.navident())
                .navIdent(ressursState.navident())
                .personident(personIdentView)
                .personIdent(personIdentView)
                .ressurstype(sektor)
                .sektor(sektor)
                .startdato(ressursState.startdato().orElse(null))
                .sluttdato(ressursState.sluttdato().orElse(null))
                .fornavn(ressursState.visningsnavn().getFornavnMellomnavn())
                .etternavn(ressursState.visningsnavn().etternavn())
                .visningsNavn(ressursState.visningsnavn().getFormattedVisningsnavn())
                .visningsnavn(ressursState.visningsnavn().getFormattedVisningsnavn())
                .build();
    }



    public static OrgEnhet toOrgEnhet(IOrgenhet orgEnhet){
        return OrgEnhet.builder()
                .id(orgEnhet.getId())
                .agressoId(orgEnhet.getAgressoId())
                .orgNiv(orgEnhet.getOrgNiv())
                .navn(orgEnhet.getNavn())
                .agressoOrgenhetType(orgEnhet.getAgressoOrgenhetType())
                .gyldigFom(orgEnhet.getGyldigFom())
                .gyldigTom(orgEnhet.getGyldigTom().orElse(null))
                .orgEnhetsKategori(orgEnhet.getNomNivaa().orElse(null))
                .nomNivaa(orgEnhet.getNomNivaa().orElse(null))
                .orgEnhetsType(orgEnhet.getOrgEnhetsType().orElse(null))
                .build();
    }

    private static String obfuscateFnr(String fnr){
        return fnr.replaceFirst(".{5}$", "*****");
    }

    private static boolean shouldObfuscatePersonIdent() {
        return loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP);
    }

}
