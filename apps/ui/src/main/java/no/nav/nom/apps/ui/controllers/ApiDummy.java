package no.nav.nom.apps.ui.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.val;

@RestController
public class ApiDummy {

    private final static ObjectMapper om = new ObjectMapper();

    @GetMapping(value="/api/**")
    public JsonNode getOrg(){
        val out =  om.createObjectNode();
        out.put("enhetsNr","1234");
        out.put("navn", "NAV Goblestad");
        out.put("enhetsType","Hovedtype");
        out.put("gyldigFra","1900-01-01");
        out.put("gyldigTil","2100-01-01");
        out.set("sti",om.createArrayNode());
        out.set("underEnheter",om.createArrayNode());

        return out;
    }
}
