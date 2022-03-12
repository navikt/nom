package no.nav.nom.apps.api.graphql.search;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SearchResolver implements GraphQLQueryResolver {

    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;

    public DataFetcherResult<List<Object>> search(String term, DataFetchingEnvironment env) {
        return SecurityUtil.<List<Object>>checkAuth(env, null).orElseGet(() -> {
            var matchedOrgenhetIds = Stream.concat(
                    orgenhetService.matchByAgressoId(term).stream(),
                    orgenhetService.matchByNavn(term).stream()
            ).map(IOrgenhet::getAgressoId).collect(Collectors.toList());

            var result = Stream.concat(
                    orgenhetService.findEnheterByAgressoIdIn(matchedOrgenhetIds).stream().map(orgenhetService::convertToGraphQlResponse),
                    ressursService.matchByNavident(term).stream().map(ressursService::convertToGraphQlResponse)
            ).collect(Collectors.toList());

            return DataFetcherResult.<List<Object>>newResult()
                    .data(result)
                    .build();
        });
    }
}
