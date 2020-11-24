package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrganisasjonsenhetResolver implements GraphQLQueryResolver {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<Organisasjonsenhet>> organisasjonsenhet(ObjectNode objectNode, DataFetchingEnvironment env) {
        return DataFetcherResult.<List<Organisasjonsenhet>>newResult()
                .data(List.of(new Organisasjonsenhet(UUID.randomUUID().toString(), "NAV Oslo Sagene", LocalDate.now(), null)))
                .build();
    }
}
