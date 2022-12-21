package no.nav.nom.apps.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhDataLoader;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@Service
@RequiredArgsConstructor
public class DvhOrgEnhetMockUtils {

    private final List<DvhOrgEnhetMock> response = new LinkedList<>();
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final DvhDataLoader dvhDataLoader;

    private void mockDvhOrgEnhetResponse(String response) {
        stubFor(get(appProperties.proxy().enhetPath()).willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    @SneakyThrows
    public void addToResponse(DvhOrgEnhetMock... enheter) {
        response.addAll(List.of(enheter));
        createResponse(response);
    }

    public void createEmptyResponse() {
        response.clear();
        createResponse(response);
    }

    public void recreateResponse() {
        createResponse(response);
    }

    @SneakyThrows
    private void createResponse(List<DvhOrgEnhetMock> orgenhetMocks) {
        val response = objectMapper.createObjectNode();
        response.set(
                "NOM_HRORG_ORGENHETResponse",
                objectMapper.createObjectNode().set("NOM_HRORG_ORGENHETResult", objectMapper.convertValue(orgenhetMocks, JsonNode.class))
        );
        mockDvhOrgEnhetResponse(objectMapper.writeValueAsString(response));
    }
}
