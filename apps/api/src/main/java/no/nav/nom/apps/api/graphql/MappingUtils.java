package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.RessursState;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;

public class MappingUtils {

    public static Ressurs toRessurs(RessursState ressursState) {
        final var sektor = singletonList(ressursState.sektor());

        var sladdetPersonIdent = ressursState.personident().replaceFirst(".{5}$","*****");
        var startdato = isNull(ressursState.startdato()) ? null : ressursState.startdato();
        var sluttdato = isNull(ressursState.sluttdato()) ? null : ressursState.sluttdato();
        var primaryTelefonnummer = isNull(ressursState.primaryTelefonnummer()) ? null : ressursState.primaryTelefonnummer();
        return Ressurs.builder()
                .navident(ressursState.navident())
                .navIdent(ressursState.navident())
                .personident(sladdetPersonIdent)
                .personIdent(sladdetPersonIdent)
                .ressurstype(sektor)
                .sektor(sektor)
                .startdato(startdato)
                .sluttdato(sluttdato)
                .fornavn(ressursState.visningsnavn().getFornavnMellomnavn())
                .etternavn(ressursState.visningsnavn().etternavn())
                .visningsNavn(ressursState.visningsnavn().getFormattedVisningsnavn())
                .visningsnavn(ressursState.visningsnavn().getFormattedVisningsnavn())
                .primaryTelefon(primaryTelefonnummer)
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
                .gyldigTom(orgEnhet.getGyldigTom())
                .orgEnhetsKategori(orgEnhet.getNomNivaa())
                .nomNivaa(orgEnhet.getNomNivaa())
                .orgEnhetsType(orgEnhet.getOrgEnhetsType())
                .build();
    }

}
