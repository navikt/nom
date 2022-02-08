package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;


@Component
@RequiredArgsConstructor
public class RessursTelefonResolver implements GraphQLResolver<Ressurs> {

    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<Telefon>> telefon(Ressurs ressurs) {

        List<Telefon> tlfList = dvhRepository.getTelefonNrListByNavIdent(ressurs.getNavIdent());
            return DataFetcherResult.<List<Telefon>>newResult()
                    .data(tlfList)
                    .build();



    }
}
