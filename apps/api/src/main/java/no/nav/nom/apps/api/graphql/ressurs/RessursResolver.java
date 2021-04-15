package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
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

    private final RessursRepository ressursRepository;

    public DataFetcherResult<Ressurs> ressurs(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<Ressurs>checkAuth(env).orElseGet(() -> {
            Optional<no.nav.nom.apps.api.domain.Ressurs> result;
            if (objectNode == null) {
                result = Optional.empty();
            } else {
                result = objectNode.findValue("navIdent") != null ?
                        ressursRepository.findByNavIdent(objectNode.findValue("navIdent").asText()) :
                        ressursRepository.findByPersonIdent(objectNode.findValue("personIdent").asText());
            }

            return DataFetcherResult.<Ressurs>newResult()
                    .data(result.map(ConverterUtils::convert).orElse(null))
                    .build();
        });
    }

    public DataFetcherResult<List<RessursResult>> ressurser(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<List<RessursResult>>checkAuth(env).orElseGet(() -> {
            var result = new ArrayList<Result>();
            List<String> navIdenter = toStringList(objectNode, "navIdenter");
            List<String> personIdenter = toStringList(objectNode, "personIdenter");
            if (objectNode == null) {
                ressursRepository.getAlleRessurser().forEach(r -> result.add(new Result(r.getNavIdent(), r)));
            } else {
                navIdenter.forEach(ident -> result.add(new Result(ident, ressursRepository.findByNavIdent(ident).orElse(null))));
                personIdenter.forEach(ident -> result.add(new Result(ident, ressursRepository.findByPersonIdent(ident).orElse(null))));
            }

            return DataFetcherResult.<List<RessursResult>>newResult()
                    .data(result.stream()
                            .map(r -> new RessursResult(r.getRessursResponse(), r.getId()))
                            .collect(Collectors.toList())
                    )
                    .build();
        });
    }

    @Value
    private static class Result {

        String id;
        no.nav.nom.apps.api.domain.Ressurs ressurs;

        public Ressurs getRessursResponse() {
            return ressurs == null ? null : ConverterUtils.convert(ressurs);
        }
    }

}
