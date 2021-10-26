package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import no.nav.nom.contract.nom.api.graphql.TelefonType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;


@Component
public class RessursTelefonResolver implements GraphQLResolver<Ressurs> {

    public DataFetcherResult<List<Telefon>> telefon(Ressurs ressurs) {

        List<Telefon> result;

        if(ressurs.getNavIdent().toUpperCase().equals("H156585")){
            result = List.of(Telefon.builder()
                    .nummer("12345678")
                    .type(TelefonType.NAV_TJENESTE_TELEFON)
                    .beskrivelse("Bent sin tjenesteTelefon")
                    .build()
            );
        }else {
            result = Collections.emptyList();
        }


        return DataFetcherResult.<List<Telefon>>newResult()
                .data(result)
                .build();
    }
}
