package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.common.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.graphql.common.ConverterUtils.toStringList;

@Component
@RequiredArgsConstructor
class OrgEnhetResolver implements GraphQLQueryResolver {

    private final IOrgenhetService orgenhetService;
    private final SecurityUtil securityUtil;
    private final GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    public DataFetcherResult<OrgEnhet> orgEnhet(ObjectNode objectNode, DataFetchingEnvironment env) {
        return securityUtil.<OrgEnhet>checkAuth(env, null).orElseGet(() -> {
            graphQLFieldConsumptionCounter.count(env);
            Optional<IOrgenhet> result;
            if (objectNode != null && objectNode.hasNonNull("agressoId") && objectNode.hasNonNull("orgNiv")) {
                val orgNiv = objectNode.findValue("orgNiv").asText();
                val agressoId = objectNode.findValue("agressoId").asText();
                val enhetNavn = objectNode.hasNonNull("navn") ? objectNode.findValue("navn").asText() : null;
                result = orgenhetService.getEnhetByAgressoIdNivaAndName(agressoId, orgNiv, enhetNavn);
            } else {
                result = Optional.empty();
            }
            return DataFetcherResult.<OrgEnhet>newResult()
                    .data(result.map(ConverterUtils::convertToGraphQlResponse).orElse(null))
                    .build();
        });
    }

    public DataFetcherResult<OrgEnhet> organisasjonsenhet(ObjectNode objectNode, DataFetchingEnvironment env) {
        return orgEnhet(objectNode,  env);
    }

    public DataFetcherResult<List<OrgEnhetResult>> orgEnheter(ObjectNode objectNode, DataFetchingEnvironment env) {
        return securityUtil.<List<OrgEnhetResult>>checkAuth(env, null).orElseGet(() -> {
            graphQLFieldConsumptionCounter.count(env);
            Collection<IOrgenhet> result;
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
            return DataFetcherResult.<List<OrgEnhetResult>>newResult()
                    .data(Stream.concat(
                            result.stream().map(ConverterUtils::convertToGraphQlResponse).map(OrgEnhetResult::new),
                            agressoIder.stream().map(id -> new OrgEnhetResult(id, orgNiv))
                    ).collect(Collectors.toList()))
                    .build();
        });
    }

    public DataFetcherResult<List<OrgEnhetResult>> organisasjonsenheter(ObjectNode objectNode, DataFetchingEnvironment env) {
        return orgEnheter(objectNode, env);
    }
}
