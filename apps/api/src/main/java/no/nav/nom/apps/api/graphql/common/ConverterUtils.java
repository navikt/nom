package no.nav.nom.apps.api.graphql.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.com.google.common.collect.Streams;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;

import java.util.List;
import java.util.stream.Collectors;

public class ConverterUtils {
    public static OrgEnhet convertToGraphQlResponse(IOrgenhet orgenhet) {
        return OrgEnhet.builder()
                .id(orgenhet.getFid().toString())
                .agressoId(orgenhet.getAgressoId())
                .orgNiv(orgenhet.getOrgNiv())
                .navn(orgenhet.getNavn())
                .agressoOrgenhetType(orgenhet.getAgressoOrgenhetType())
                .gyldigFom(orgenhet.getGyldigFom())
                .gyldigTom(orgenhet.getGyldigTom().orElse(null))
                .build();
    }

    public static List<String> toStringList(ObjectNode objectNode, String fieldName) {
        if (objectNode == null) {
            return List.of();
        }
        ArrayNode list = (ArrayNode) objectNode.findValue(fieldName);
        if (list == null) {
            return List.of();
        }
        return Streams.stream(list.elements()).map(JsonNode::asText).collect(Collectors.toList());
    }
}
