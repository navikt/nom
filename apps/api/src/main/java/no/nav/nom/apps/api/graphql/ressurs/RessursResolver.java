package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.apps.api.ressurs.contract.RessursService;
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

    private final RessursService ressursService;

    public DataFetcherResult<Ressurs> ressurs(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<Ressurs>checkAuth(env, null).orElseGet(() -> {
            Optional<RessursI> result;
            if (objectNode == null) {
                result = Optional.empty();
            } else {
                result = objectNode.findValue("navIdent") != null ?
                        ressursService.findByNavIdent(objectNode.findValue("navIdent").asText()) :
                        ressursService.findByPersonIdent(objectNode.findValue("personIdent").asText());
            }

            return DataFetcherResult.<Ressurs>newResult()
                    .data(result.map(ressursService::convertToGraphQlResponse).orElse(null))
                    .build();
        });
    }

    public DataFetcherResult<List<RessursResult>> ressurser(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<List<RessursResult>>checkAuth(env, null).orElseGet(() -> {
            var result = new ArrayList<Result>();
            List<String> navIdenter = toStringList(objectNode, "navIdenter");
            List<String> personIdenter = toStringList(objectNode, "personIdenter");
            if (objectNode == null) {
                ressursService.getAll().forEach(r -> result.add(new Result(r.getNavIdent(), r)));
            } else {
                navIdenter.forEach(ident -> result.add(new Result(ident, ressursService.findByNavIdent(ident).orElse(null))));
                personIdenter.forEach(ident -> result.add(new Result(ident, ressursService.findByPersonIdent(ident).orElse(null))));
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
