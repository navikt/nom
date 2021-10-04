package no.nav.nom.apps.api.graphql.person;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.domain.RessursService;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.contract.nom.api.graphql.Person;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PersonResolver implements GraphQLQueryResolver {

    private final RessursService ressursService;

    public DataFetcherResult<PersonResult> person(ObjectNode objectNode, DataFetchingEnvironment env) {
        /*return SecurityUtil.<PersonResult>checkAuth(env).orElseGet(() -> {
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
        });*/

        return DataFetcherResult.<PersonResult>newResult()
                .error(ErrorCode.UNAUTHENTICATED.construct(env, "Missing or invalid token"))
                .build();
    }
}
