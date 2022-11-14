package no.nav.nom.apps.api.graphql;

import lombok.AllArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import no.nav.nom.contract.nom.state.RessursState;

import static java.util.Collections.singletonList;

public class MappingUtils {

    public static Ressurs toRessurs(RessursState ressursState){
        final var sektor = singletonList(Sektor.from(ressursState.sektor()));

        return Ressurs.builder()
                .navident(ressursState.navident())
                .navIdent(ressursState.navident())
                .personident(ressursState.personident())
                .personIdent(ressursState.personident())
                .ressurstype(sektor)
                .sektor(sektor)
                .sluttdato(ressursState.sluttdato().orElse(null))
                .fornavn(ressursState.visningsnavn().mellomnavn().map(m -> ressursState.visningsnavn().fornavn()+" "+m).orElse(ressursState.visningsnavn().fornavn()))
                .etternavn(ressursState.visningsnavn().etternavn())
                .build();
    }

    public static Ressurs toRessurs(IRessurs iressurs){
        final var sektor = singletonList(Sektor.valueOf(iressurs.getSektor()));

        return Ressurs.builder()
                .navident(iressurs.getNavIdent())
                .navIdent(iressurs.getNavIdent())
                .personident(iressurs.getPersonIdent())
                .personIdent(iressurs.getPersonIdent())
                .ressurstype(sektor)
                .sektor(sektor)
                .build();
    }

}
