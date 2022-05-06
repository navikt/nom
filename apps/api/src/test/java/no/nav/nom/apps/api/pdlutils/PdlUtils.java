package no.nav.nom.apps.api.pdlutils;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Component
@RequiredArgsConstructor
public class PdlUtils {
    private final Map<String, HentPersonBulkEntry> pdlEntryByIdent = new HashMap<>();
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public void assertNavn(String personIdent, JsonNode navn) {
        val navnActual = pdlEntryByIdent.get(personIdent).getPerson().getNavn().get(0);
        assertEquals(navnActual.getFornavn(), navn.get("fornavn").asText());
        assertEquals(navnActual.getEtternavn(), navn.get("etternavn").asText());

        if (navnActual.getMellomnavn() != null) {
            assertEquals(navnActual.getMellomnavn(), navn.get("mellomnavn").asText());
        } else {
            assertFalse(navn.hasNonNull("mellomnavn"));
        }
    }

    private void mockPdlResponse(String response) {
        stubFor(post("/graphql")
                .withHeader("Tema", equalTo(appProperties.getPdl().getTema()))
                .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(response)));
    }

    public void addPersonToPdlResponse(boolean mellomnavn, String... personIdenter) {
        List.of(personIdenter).forEach(p -> {
            val navn = NavnDto.builder()
                    .fornavn("fornavn"+pdlEntryByIdent.size())
                    .mellomnavn(mellomnavn ? "mellomnavn"+pdlEntryByIdent.size() : null)
                    .etternavn("etternavn"+pdlEntryByIdent.size())
                    .build();
            val entry = HentPersonBulkEntry.builder()
                    .ident(p)
                    .person(new PersonDto(List.of(navn)))
                    .code("ok")
                    .build();
            pdlEntryByIdent.put(p, entry);
        });

        mockPdlResponse(buildResponse());
    }

    @SneakyThrows
    public NavnDto mockSinglePersonResponse(String personIdent){
        val resultBuilder = new StringBuilder();

        val navn = NavnDto.builder()
                .fornavn("fornavn")
                .mellomnavn("mellomnavn")
                .etternavn("etternavn")
                .build();

        resultBuilder.append("{\"data\": {\"hentPerson\": { \"navn\": [");
        resultBuilder.append(objectMapper.writeValueAsString(navn));
        mockPdlResponse(resultBuilder.append("]}}}").toString());

        return navn;
    }

    public void addErrorToPdlResponse(String personIdent, String code) {
        val entry = HentPersonBulkEntry.builder()
                .ident(personIdent)
                .person(null)
                .code(code)
                .build();
        pdlEntryByIdent.put(personIdent, entry);

        mockPdlResponse(buildResponse());
    }

    @SneakyThrows
    private String buildResponse() {
        val resultBuilder = new StringBuilder();
        resultBuilder.append("{\"data\": {\"hentPersonBolk\": ");
        resultBuilder.append(objectMapper.writeValueAsString(pdlEntryByIdent.values()));

        return resultBuilder.append("}}").toString();
    }

    public void reset() {
        pdlEntryByIdent.clear();
    }
}
