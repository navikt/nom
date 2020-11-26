package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhOrganisasjonsenhet;
import no.nav.nom.apps.api.dvh.DvhRepository;
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

        if (objectNode == null) {
            return DataFetcherResult.<List<Organisasjonsenhet>>newResult()
                    .data(dvhRepository.findAllEnheter().stream().map(this::convert).collect(Collectors.toList()))
                    .build();
        }
        val result = dvhRepository.findEnhetByNummer(objectNode.findValue("id").asText())
                .map(this::convert)
                .map(List::of)
                .orElse(Collections.emptyList());
        return DataFetcherResult.<List<Organisasjonsenhet>>newResult()
                .data(result)
                .build();
    }

    private Organisasjonsenhet convert(DvhOrganisasjonsenhet dvhOrganisasjonsenhet){
        return Organisasjonsenhet.builder()
                .id(dvhOrganisasjonsenhet.getNummer())
                .navn(dvhOrganisasjonsenhet.getNavn())
                .gyldigFom(dvhOrganisasjonsenhet.getGyldigFom())
                .gyldigTil(dvhOrganisasjonsenhet.getGyldigTil())
                .build();
    }
}
