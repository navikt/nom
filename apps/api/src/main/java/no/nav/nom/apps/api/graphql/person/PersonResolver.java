package no.nav.nom.apps.api.graphql.person;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PersonResolver implements GraphQLQueryResolver {

    private final IRessursService ressursService;

    @Value("${app.graphql.preauth.query.person}")
    private List<String> preauth;

    public DataFetcherResult<PersonResult> person(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<PersonResult>checkAuth(env, preauth).orElseGet(() -> {
            var personIdentOptional = Optional.ofNullable(objectNode).map(n -> n.findValue("personIdent").asText());
            if (personIdentOptional.isEmpty()) {
                return DataFetcherResult.<PersonResult>newResult()
                        .data(null)
                        .build();
            }
            String personIdent = personIdentOptional.get();
            var ressursResult = ressursService.findByPersonIdent(personIdent);

            var personResult = PersonResult.builder().personIdent(personIdent);
            ressursResult.ifPresent(res -> personResult.nomId(res.getId()).navIdent(res.getNavIdent()));

            return DataFetcherResult.<PersonResult>newResult()
                    .data(personResult.build())
                    .build();
        });
    }
}
