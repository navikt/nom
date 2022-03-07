package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.orgenhet.contract.OrgenhetService;
import no.nav.nom.contract.nom.api.graphql.LederOrganisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RessursLederForResolver implements GraphQLResolver<Ressurs> {

    private final DvhRepository dvhRepository;
    private final OrgenhetService orgenhetService;

    public DataFetcherResult<List<LederOrganisasjonsenhet>> lederFor(Ressurs ressurs) {
        var result = dvhRepository.getEnheterByLeder(ressurs.getNavIdent())
                .stream()
                .map(orgenhetService::convertToGraphQlResponse)
                .map(LederOrganisasjonsenhet::new)
                .toList();

        return DataFetcherResult.<List<LederOrganisasjonsenhet>>newResult()
                .data(result)
                .build();
    }

}
