package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@Component
@RequiredArgsConstructor
public class DvhOrganiseringMockUtils {

    private final List<DvhOrganiseringMock> response = new LinkedList<>();
    private final ObjectMapper objectMapper;

    public void addToResponse(DvhOrganiseringMock... organiseringer) throws JsonProcessingException {
        response.addAll(List.of(organiseringer));
        createResponse(response);
    }

    private void createResponse(List<DvhOrganiseringMock> dvhOrganiseringMocks) throws JsonProcessingException {
        val response = objectMapper.createObjectNode();
        response.set("NOM_HRORG_HIERARKI2_v2Response", objectMapper.convertValue(dvhOrganiseringMocks, JsonNode.class));
        mockDvhOrgEnhetResponse(objectMapper.writeValueAsString(response));
    }

    private void mockDvhOrgEnhetResponse(String response) {
        stubFor(get("/proxy/dvh/json/Hr/NOM2/NOM_HRORG_HIERARKI2_v2").willReturn(aResponse().withHeader("Content-Type", "application/json")
            .withBody(response)));
    }


}
