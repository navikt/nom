package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.contract.Orgenhet;
import no.nav.nom.apps.api.orgenhet.contract.OrgenhetService;
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

    private final OrgenhetService orgenhetService;

    public DataFetcherResult<Organisasjonsenhet> organisasjonsenhet(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<Organisasjonsenhet>checkAuth(env, null).orElseGet(() -> {
            Optional<Orgenhet> result;
            if (objectNode != null && objectNode.hasNonNull("agressoId") && objectNode.hasNonNull("orgNiv")) {
                val orgNiv = objectNode.findValue("orgNiv").asText();
                val agressoId = objectNode.findValue("agressoId").asText();
                result = orgenhetService.getEnhetByAgressoIdNivaAndName(agressoId, orgNiv, null);
            } else {
                result = Optional.empty();
            }
            return DataFetcherResult.<Organisasjonsenhet>newResult()
                    .data(result.map(orgenhetService::convertToGraphQlResponse).orElse(null))
                    .build();
        });
    }

    public DataFetcherResult<List<OrganisasjonsenhetResult>> organisasjonsenheter(ObjectNode objectNode, DataFetchingEnvironment env) {
        return SecurityUtil.<List<OrganisasjonsenhetResult>>checkAuth(env, null).orElseGet(() -> {
            Collection<Orgenhet> result;
            val agressoIder = toStringList(objectNode, "agressoIder");
            val orgNiv = objectNode != null && objectNode.hasNonNull("orgNiv") ? objectNode.get("orgNiv").asText() : null;
            if (objectNode == null || agressoIder.isEmpty()) {
                result = orgenhetService.getAllEnheter().stream()
                        .filter(e -> orgNiv == null || orgNiv.equals(e.getOrgNiv()))
                        .toList();
            } else {
                result = orgenhetService.findEnheterByAgressoIdIn(agressoIder).stream()
                        .filter(e -> orgNiv == null || orgNiv.equals(e.getOrgNiv())).toList();
                result.forEach(e -> agressoIder.remove(e.getAgressoId()));
            }
            return DataFetcherResult.<List<OrganisasjonsenhetResult>>newResult()
                    .data(Stream.concat(
                            result.stream().map(orgenhetService::convertToGraphQlResponse).map(OrganisasjonsenhetResult::new),
                            agressoIder.stream().map(id -> new OrganisasjonsenhetResult(id, orgNiv))
                    ).collect(Collectors.toList()))
                    .build();
        });
    }
}
