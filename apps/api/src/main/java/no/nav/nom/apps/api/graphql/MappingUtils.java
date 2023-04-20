package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.RessursState;

import static java.util.Collections.singletonList;

public class MappingUtils {

    public static Ressurs toRessurs(RessursState ressursState) {
        final var sektor = singletonList(ressursState.sektor());

        return Ressurs.builder()
                .navident(ressursState.navident())
                .navIdent(ressursState.navident())
                .personident(ressursState.personident())
                .personIdent(ressursState.personident())
                .ressurstype(sektor)
                .sektor(sektor)
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
                .orgEnhetsKategori(orgEnhet.getOrgEnhetsKategori().orElse(null))
                .orgEnhetsType(orgEnhet.getOrgEnhetsType().orElse(null))
                .build();
    }

}
