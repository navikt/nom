package no.nav.nom.apps.api.graphql.search;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.domain.Orgenhet;
import no.nav.nom.apps.api.domain.OrgenhetService;
import no.nav.nom.apps.api.domain.RessursService;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.domain.Orgenhet.AGRESSO_ID_FIELD;
import static no.nav.nom.apps.api.domain.Orgenhet.NAVN_FIELD;

@Component
@RequiredArgsConstructor
public class SearchResolver implements GraphQLQueryResolver {

    private final DvhRepository dvhRepository;
    private final RessursService ressursService;
    private final OrgenhetService orgenhetService;

    public DataFetcherResult<List<Object>> search(String term, DataFetchingEnvironment env) {
        return SecurityUtil.<List<Object>>checkAuth(env).orElseGet(() -> {
            var matchedOrgenhetIds = Stream.concat(
                    orgenhetService.matchBy(AGRESSO_ID_FIELD, term).stream(),
                    orgenhetService.matchBy(NAVN_FIELD, term.toLowerCase()).stream()
            ).map(Orgenhet::getAgressoId).collect(Collectors.toList());

            var result = Stream.concat(
                    dvhRepository.findEnheterByAgressoIdIn(matchedOrgenhetIds).stream().map(ConverterUtils::convert),
                    ressursService.matchByNavident(term).stream().map(ConverterUtils::convert)
            ).collect(Collectors.toList());

            return DataFetcherResult.<List<Object>>newResult()
                    .data(result)
                    .build();
        });
    }
}
