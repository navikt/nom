package no.nav.nom.apps.api.graphql.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.com.google.common.collect.Streams;
import no.nav.nom.apps.api.domain.OrgType;
import no.nav.nom.apps.api.domain.OrgTyper;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Kode;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

import java.util.List;
import java.util.stream.Collectors;

public class ConverterUtils {

    public static Organisasjonsenhet convert(DvhOrganisasjonsenhet orgEnht) {
        return Organisasjonsenhet.builder()
                .agressoId(orgEnht.getAgressoId())
                .orgNiv(orgEnht.getOrgNiv())
                .navn(orgEnht.getNavn())
                .gyldigFom(orgEnht.getGyldigFom())
                .gyldigTom(orgEnht.getGyldigTom())
                .type(kodeFra(OrgTyper.forAgressoOrgType(orgEnht.getAgressoOrgType())))
                .build();
    }

    public static Ressurs convert(no.nav.nom.apps.api.domain.Ressurs ressurs) {
        return Ressurs.builder()
                .nomId(ressurs.getId())
                .navIdent(ressurs.getNavIdent())
                .personIdent(ressurs.getPersonIdent())
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

    private static Kode kodeFra(OrgType orgType) {
        return orgType == null ? null :
                Kode.builder()
                        .kode(orgType.getKode())
                        .navn(orgType.getNavn())
                        .gyldigFom(orgType.getGyldigFom())
                        .gyldigTom(orgType.getGyldigTom())
                        .build();
    }

}
