package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetResult;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.graphql.common.ConverterUtils.toStringList;

@Component
@RequiredArgsConstructor
class OrganisasjonsenhetResolver implements GraphQLQueryResolver {

    private final DvhRepository dvhRepository;

    public DataFetcherResult<Organisasjonsenhet> organisasjonsenhet(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<Organisasjonsenhet>checkAuth(env).orElseGet(() -> {
            Optional<DvhOrganisasjonsenhet> result;
            if (objectNode == null) {
                result = Optional.empty();
            } else {
                result = dvhRepository.getEnhetByAgressoId(objectNode.findValue("agressoId").asText());
            }
            return DataFetcherResult.<Organisasjonsenhet>newResult()
                    .data(result.map(ConverterUtils::convert).orElse(null))
                    .build();
        });
    }

    public DataFetcherResult<List<OrganisasjonsenhetResult>> organisasjonsenheter(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<List<OrganisasjonsenhetResult>>checkAuth(env).orElseGet(() -> {
            Collection<DvhOrganisasjonsenhet> result;
            List<String> agressoIder = toStringList(objectNode, "agressoIder");
            if (objectNode == null) {
                result = dvhRepository.getAllEnheter();
            } else {
                result = dvhRepository.findEnheterByAgressoIdIn(agressoIder);
                result.forEach(e -> agressoIder.remove(e.getAgressoId()));
            }
            return DataFetcherResult.<List<OrganisasjonsenhetResult>>newResult()
                    .data(Stream.concat(
                            result.stream().map(ConverterUtils::convert).map(OrganisasjonsenhetResult::new),
                            agressoIder.stream().map(OrganisasjonsenhetResult::new)
                    ).collect(Collectors.toList()))
                    .build();
        });
    }
}
