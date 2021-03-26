package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.common.CustomGraphQLContext;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class RessursResolver implements GraphQLQueryResolver {

    private final RessursRepository ressursRepository;

    public DataFetcherResult<List<Ressurs>> ressurs(ObjectNode objectNode, DataFetchingEnvironment env) {
        if (((CustomGraphQLContext) env.getContext()).isAuthenticated()) {
            if (objectNode == null) {
                return DataFetcherResult.<List<Ressurs>>newResult()
                        .data(ressursRepository.getAlleRessurser().stream()
                                .map(ConverterUtils::convert)
                                .collect(Collectors.toUnmodifiableList()))
                        .build();
            }

            val result = objectNode.findValue("navIdent") != null ?
                    ressursRepository.findByNavIdent(objectNode.findValue("navIdent").asText())
                            .map(ConverterUtils::convert)
                            .map(List::of)
                            .orElse(Collections.emptyList()) :
                    ressursRepository.findByPersonIdent(objectNode.findValue("personIdent").asText())
                            .map(ConverterUtils::convert)
                            .map(List::of)
                            .orElse(Collections.emptyList());

            return DataFetcherResult.<List<Ressurs>>newResult()
                    .data(result)
                    .build();
        } else {
            return DataFetcherResult.<List<Ressurs>>newResult()
                    .error(ErrorCode.UNAUTHENTICATED.construct(env, "Missing or invalid token"))
                    .build();
        }
    }
}
