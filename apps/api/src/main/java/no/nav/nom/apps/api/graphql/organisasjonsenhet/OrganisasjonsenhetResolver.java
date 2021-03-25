package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.CustomGraphQLContext;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class OrganisasjonsenhetResolver implements GraphQLQueryResolver {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<Organisasjonsenhet>> organisasjonsenhet(ObjectNode objectNode, DataFetchingEnvironment env) {
        if (((CustomGraphQLContext) env.getContext()).isAuthenticated()) {
            if (objectNode == null) {
                return DataFetcherResult.<List<Organisasjonsenhet>>newResult()
                        .data(dvhRepository.getAllEnheter().stream().map(ConverterUtils::convert).collect(Collectors.toList()))
                        .build();
            }

            val result = dvhRepository.getEnhetByAgressoId(objectNode.findValue("agressoId").asText())
                    .map(ConverterUtils::convert)
                    .map(List::of)
                    .orElse(Collections.emptyList());
            return DataFetcherResult.<List<Organisasjonsenhet>>newResult()
                    .data(result)
                    .build();
        } else {
            return DataFetcherResult.<List<Organisasjonsenhet>>newResult()
                    .error(ErrorCode.UNAUTHENTICATED.construct(env, "Missing or invalid token"))
                    .build();
        }
    }
}
