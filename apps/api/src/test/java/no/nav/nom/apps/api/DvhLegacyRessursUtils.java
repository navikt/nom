package no.nav.nom.apps.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.ressurs.implementations.dvh.LegacyRessursLoader;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@Component
public class DvhLegacyRessursUtils {

    private final ObjectMapper objectMapper;
    private final List<LegacyResponseObject> responseObjects = new LinkedList<>();
    private final LegacyRessursLoader legacyRessursLoader;

    private long currentOffset = 0; //Endepunktet starter indeksering fra 1
    private long lastOffset = 0;
    private long incrementValue = 5000;

    public DvhLegacyRessursUtils(ObjectMapper objectMapper, LegacyRessursLoader legacyRessursLoader){
        this.objectMapper = objectMapper;
        this.legacyRessursLoader = legacyRessursLoader;
    }


    public void addToResponse(LegacyResponseObject legacyResponseObject){
        legacyResponseObject.setOffset(++currentOffset);
        responseObjects.add(legacyResponseObject);
    }
    public void addToResponse(List<LegacyResponseObject> legacyResponseObjects){
        legacyResponseObjects.forEach(o -> o.setOffset(++currentOffset));
        responseObjects.addAll(legacyResponseObjects);
    }

    public synchronized void mockResponse(){
        mockResponse(responseObjects, lastOffset, incrementValue+lastOffset);
        lastOffset = currentOffset;
        mockResponse(Collections.emptyList(), 0+currentOffset, incrementValue+currentOffset);

        legacyRessursLoader.load();
        responseObjects.clear();
    }

    @SneakyThrows
    private StubMapping mockResponse(List<LegacyResponseObject> responseObjects, long sekFrom, long sekTo){
        var body = objectMapper.createObjectNode().set("Nom_AnsatteResponse",
                objectMapper.createObjectNode().set("Nom_AnsatteResult", objectMapper.convertValue(responseObjects, JsonNode.class)));

        return stubFor(get("/proxy/dvh/json/Hr/Nom/Nom_Ansatte?in_sekvensnummer_fra="+sekFrom+"&in_sekvensnummer_til="+sekTo)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body.toString())
                )
        );
    }

    @Getter
    @Setter
    @Builder
    public static class LegacyResponseObject{
        @JsonProperty("SEKVENSNR")
        private Long offset;
        @JsonProperty("FNR")
        private String personIdent;
        @JsonProperty("NAV_IDENT")
        private String navIdent;
        @JsonProperty("ANSATT_RESSURS_KODE")
        private String agressoRessursType;
        @JsonProperty("ANSATT_TYPE")
        private String remedyRessursType;
        @JsonProperty("REMEDY_SLUTT_DATO")
        private LocalDate remedySluttdato;
        @JsonProperty("AGRESSO_SLUTT_DATO")
        private LocalDate agressoSluttdato;
        @JsonProperty("REMEDY_PERMISJON")
        private String remedyPermisjon;
        @JsonIgnore
        private LocalDate forventetSluttdato;
    }
}
