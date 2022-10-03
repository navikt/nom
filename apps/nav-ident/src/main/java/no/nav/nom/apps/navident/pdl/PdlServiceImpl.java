package no.nav.nom.apps.navident.pdl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.security.AADTokenRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdlServiceImpl implements PdlService {

    private final String pdlHentIdentQry;

    private final AADTokenRepository tokenRepository;
    private final WebClient pdlWebClient;
    private final PdlConfig pdlConfig;

    private final ObjectMapper mapper = new ObjectMapper();

    // If anything fails in PDL communication/query, we return Empty list.
    // rnr Flow will then genereate new id as if nothing bad has happened

    @Override
    public List<String> getKnownPersonider(String id) {
        String token = tokenRepository.getOboOrClientCredToken(pdlConfig.getAadClientId());
        if (token == null) {
            log.error("Fikk ingen token fra tokenRepository. Skipper oppslag mot PDL");
            return Collections.emptyList();
        }

        String qryBody = getQuery(pdlHentIdentQry, "ident", id).toPrettyString();

        String pdlResponse = postToPdl(qryBody, token);

        if (pdlResponse == null) {
            log.error(String.format("PDL GraphQL returned empty response for qry:\n%s\nwith ident = %s***", pdlHentIdentQry, id.substring(0,5)));
            return Collections.emptyList();
        }

        JsonNode response;
        try {
            response = mapper.readTree(pdlResponse);
        } catch (JsonProcessingException e) {
            log.error(String.format("PDL GraphQL returned unparsable json response for qry:\n%s\nwith ident = %s***", pdlHentIdentQry, id.substring(0,5)));
            return Collections.emptyList();
        }

        if (response.path("errors").isMissingNode()) {
            try {
                List<PdlIdent> identer = Arrays.asList(mapper.treeToValue(response.findPath("identer"), PdlIdent[].class));
                return identer.stream()
                        .filter( PdlIdent::erFolkeregisterId )
                        .sorted( Comparator.comparing(PdlIdent::erGjeldende, Comparator.reverseOrder()) )   // Sorter slik at gjeldende folkeregistrert id for personen ligger først
                        .map( i -> i.ident ).collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                log.error(String.format("Not able to convert PDL GraphQL response HentIdenterResponse.class for qry:\n%s\nwith ident = %s***", pdlHentIdentQry, id.substring(0,5)));
                return Collections.emptyList();
            }
        } else {
            String errorMessage = response.withArray("errors").get(0).path("message").textValue();
            log.error(String.format("PDL GraphQL returned error in response: %s\nfor qry:\n%s\nwith ident = %s***", errorMessage, pdlHentIdentQry, id.substring(0,5)));
            return Collections.emptyList();
        }
    }

    private String postToPdl(String qryBody, String token) {
        return pdlWebClient
                .post()
                .uri(pdlConfig.getBaseUrl() + "/graphql")
                .headers(headers -> {
                    headers.add(HEADER_CALL_ID_KEY, MDC.get(MDC_CALL_ID_KEY));
                    headers.add(HEADER_CONSUMER_ID_KEY, MDC.get(MDC_CONSUMER_ID_KEY));
                    headers.add(HEADER_X_REQUEST_ID_KEY, MDC.get(HEADER_X_REQUEST_ID_KEY));
                    headers.setBearerAuth(token);
                })
                .body(Mono.just(qryBody), String.class)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private JsonNode getQuery(String qry, String varialbleName, String variableValue) {
        return getQuery(qry, varialbleName, Collections.singletonList(variableValue));
    }

    private JsonNode getQuery(String qry, String varialbleName, List<String> variableValues) {
        return getQuery(qry, Collections.singletonMap(varialbleName, variableValues));
    }

    private JsonNode getQuery(String qry, Map<String, List<String>> variables) {
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode variablesNode = mapper.createObjectNode();

        variables.forEach( (name, values) -> variablesNode.setAll( getVariableNode(name, values) ));
        rootNode
                .put("query", qry)
                .set("variables", variablesNode);
        return rootNode;
    }

    private ObjectNode getVariableNode(String name, List<String> values) {
        ObjectNode node = mapper.createObjectNode();
        if (values.size() == 1) {
            node.put(name, values.get(0));
        } else {
            ArrayNode array = mapper.createArrayNode();
            values.forEach(array::add);
            node.set(name, array);
        }
        return node;
    }


}
