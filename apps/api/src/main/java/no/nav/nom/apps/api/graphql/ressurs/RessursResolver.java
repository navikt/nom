package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RessursResolver implements GraphQLQueryResolver {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<Ressurs>> ressurs(ObjectNode objectNode, DataFetchingEnvironment env) {

        if (objectNode == null) {
            return DataFetcherResult.<List<Ressurs>>newResult()
                    .data(dvhRepository.findAllRessurser().stream().map(this::convert).collect(Collectors.toUnmodifiableList()))
                    .build();
        }

        val result = objectNode.findValue("navIdent") != null ?
                            dvhRepository.findByNavIdent(objectNode.findValue("navIdent").asText())
                                    .map(this::convert)
                                    .map(List::of)
                                    .orElse(Collections.emptyList()) :
                            dvhRepository.findByPersonIdent(objectNode.findValue("personIdent").asText())
                                    .map(this::convert)
                                    .map(List::of)
                                    .orElse(Collections.emptyList());

        return DataFetcherResult.<List<Ressurs>>newResult()
                .data(result)
                .build();
    }

    private Ressurs convert(DvhRessurs dvhRessurs){
        return Ressurs.builder()
                .navIdent(dvhRessurs.getNavIdent())
                .personIdent(dvhRessurs.getPersonIdent())
                .build();
    }
}
