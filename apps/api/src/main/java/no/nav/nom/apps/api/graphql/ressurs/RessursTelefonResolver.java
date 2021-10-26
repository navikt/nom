package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;


@Component
public class RessursTelefonResolver implements GraphQLResolver<Ressurs> {

    public DataFetcherResult<List<Telefon>> telefon(Ressurs ressurs) {

        return DataFetcherResult.<List<Telefon>>newResult()
                .data(Collections.emptyList())
                .build();
    }
}
