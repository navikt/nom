package no.nav.nom.apps.api.graphql.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.com.google.common.collect.Streams;
import no.nav.nom.apps.api.graphql.request.Ordre;
import no.nav.nom.apps.api.graphql.request.dto.OrdreLinje;

import java.util.List;
import java.util.stream.Collectors;

public class ConverterUtils {

    public static Ordre convert(no.nav.nom.apps.api.repository.ordre.Ordre ordre){
        return null;
    }

    public static List<OrdreLinje> convert(JsonNode ordreLinje){
        return null;
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
