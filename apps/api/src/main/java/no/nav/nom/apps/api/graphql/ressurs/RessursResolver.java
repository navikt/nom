package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import no.nav.nom.apps.api.graphql.common.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.graphql.common.ConverterUtils.toStringList;

@Component
@RequiredArgsConstructor
class RessursResolver implements GraphQLQueryResolver {

    private final IRessursService ressursService;
    private final SecurityUtil securityUtil;
    private final GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    public DataFetcherResult<Ressurs> ressurs(ObjectNode objectNode, DataFetchingEnvironment env) {
        return securityUtil.<Ressurs>checkAuth(env, null).orElseGet(() -> {
            graphQLFieldConsumptionCounter.count(env);
            Optional<RessursI> result;
            if (objectNode == null) {
                result = Optional.empty();
            } else {
                val navident = Optional
                        .ofNullable(objectNode.findValue("navident"))
                        .orElse(objectNode.findValue("navIdent"));
                val personident = Optional
                        .ofNullable(objectNode.findValue("personident"))
                        .orElse(objectNode.findValue("personIdent"));
                result = navident != null ?
                        ressursService.findByNavIdent(navident.asText()) :
                        ressursService.findByPersonIdent(personident.asText());
            }

            return DataFetcherResult.<Ressurs>newResult()
                    .data(result.map(ressursService::convertToGraphQlResponse).orElse(null))
                    .build();
        });
    }

    public DataFetcherResult<List<RessursResult>> ressurser(ObjectNode objectNode, DataFetchingEnvironment env) {
        return securityUtil.<List<RessursResult>>checkAuth(env, null).orElseGet(() -> {
            graphQLFieldConsumptionCounter.count(env);
            var result = new ArrayList<Result>();

            if (objectNode == null) {
                ressursService.getAll().forEach(r -> result.add(new Result(r.getNavIdent(), r)));
            } else {
                val navidenter = objectNode.findValue("navIdenter") != null
                        ? toStringList(objectNode, "navIdenter")
                        : toStringList(objectNode, "navidenter");
                val personidenter = objectNode.findValue("personIdenter") != null
                        ? toStringList(objectNode, "personIdenter")
                        : toStringList(objectNode, "personidenter");
                navidenter.forEach(ident -> result.add(new Result(ident, ressursService.findByNavIdent(ident).orElse(null))));
                personidenter.forEach(ident -> result.add(new Result(ident, ressursService.findByPersonIdent(ident).orElse(null))));
            }

            return DataFetcherResult.<List<RessursResult>>newResult()
                    .data(result.stream()
                            .map(r -> new RessursResult(r.getRessursResponse() != null ? ressursService.convertToGraphQlResponse(r.getRessursResponse()) : null, r.getId()))
                            .collect(Collectors.toList())
                    )
                    .build();
        });
    }

    @Value
    private static class Result {

        String id;
        RessursI ressurs;

        public RessursI getRessursResponse() {
            return ressurs;
        }
    }

}
