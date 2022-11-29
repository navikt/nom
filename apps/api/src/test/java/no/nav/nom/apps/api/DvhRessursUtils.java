package no.nav.nom.apps.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@Component
public class DvhRessursUtils {

    private final ObjectMapper objectMapper;
    private final List<RessursMock> ressursMocks = new LinkedList<>();

    public DvhRessursUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public void addToResponse(RessursMock ressursMock) {
        ressursMocks.add(ressursMock);
        mockResponse();
    }

    public void addToResponse(List<RessursMock> ressursMocks) {
        this.ressursMocks.addAll(ressursMocks);
        mockResponse();
    }

    private synchronized void mockResponse() {
        mockResponse(ressursMocks);
    }

    public void createEmptyResponse() {
        ressursMocks.clear();
        mockResponse(ressursMocks);
    }

    private StubMapping mockResponse(List<RessursMock> ressursMocks) {
        var body = objectMapper.createObjectNode().set("NOM_ANSATTE_BUCKETS_v1Result", objectMapper.convertValue(ressursMocks, JsonNode.class));

        return stubFor(
                get("/proxy/dvh/json/Hr/NOM2/NOM_ANSATTE_BUCKETS_v1")
                        .willReturn(
                                aResponse().withHeader("Content-Type", "application/json")
                                        .withBody(body.toString())
                        )
        );
    }

    public record RessursMock(
            @JsonProperty("NAV_IDENT") String NAV_IDENT,
            @JsonProperty("FNR") String FNR,
            @JsonProperty("FORNAVN") String FORNAVN,
            @JsonProperty("ETTERNAVN") String ETTERNAVN,
            @JsonProperty("ANSATT_TYPE") String ANSATT_TYPE,
            @JsonProperty("ANSATT_RESSURS_KODE") String ANSATT_RESSURS_KODE,
            @JsonProperty("REMEDY_AKTIV") String REMEDY_AKTIV,
            @JsonProperty("REMEDY_PERMISJON") String REMEDY_PERMISJON,
            @JsonProperty("REMEDY_START_DATO") LocalDate REMEDY_START_DATO,
            @JsonProperty("REMEDY_SLUTT_DATO") LocalDate REMEDY_SLUTT_DATO,
            @JsonProperty("AGRESSO_SLUTT_DATO") LocalDate AGRESSO_SLUTT_DATO
    ) {
        public RessursMock(
                String NAV_IDENT, String FNR, String FORNAVN, String ETTERNAVN, String ANSATT_TYPE, LocalDate REMEDY_SLUTT_DATO
        ) {
            this(NAV_IDENT, FNR, FORNAVN, ETTERNAVN, ANSATT_TYPE, "F", "JA", "NEI", LocalDate.now(), REMEDY_SLUTT_DATO, LocalDate.parse("2099-12-31"));
        }
    }
}
