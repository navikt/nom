package no.nav.nom.apps.api.graphql;

import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import no.nav.nom.contract.nom.state.RessursState;

import static java.util.Collections.singletonList;

public class MappingUtils {

    public static Ressurs toRessurs(RessursState ressursState){
        final var sektor = singletonList(Sektor.from(ressursState.sektor()));

        return Ressurs.builder()
                .nomId(ressursState.fid())
                .navident(ressursState.navident())
                .navIdent(ressursState.navident())
                .personident(ressursState.personident())
                .personIdent(ressursState.personident())
                .ressurstype(sektor)
                .sektor(sektor)
                .sluttdato(ressursState.sluttdato())
                .fornavn(ressursState.visningsnavn().mellomnavn().map(m -> ressursState.visningsnavn().fornavn()+" "+m).orElse(ressursState.visningsnavn().fornavn()))
                .etternavn(ressursState.visningsnavn().etternavn())
                .build();
    }
}
