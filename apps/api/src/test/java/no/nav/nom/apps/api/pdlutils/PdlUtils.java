package no.nav.nom.apps.api.pdlutils;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@Component
@RequiredArgsConstructor
public class PdlUtils {
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private void mockPdlResponse(String response) {
        stubFor(post("/graphql")
            .withHeader("Tema", equalTo(appProperties.clients().pdl().tema()))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    @SneakyThrows
    public NavnDTO mockSinglePersonResponse() {

        val navn = NavnDTO.builder().fornavn("fornavn").mellomnavn("mellomnavn").etternavn("etternavn").build();

        val kjoenn = KjonnDTO.builder().kjoenn(Kjonn.UKJENT).build();

        val foedselsdato = objectMapper.createObjectNode()
                .put("foedselsdato", LocalDate.now().toString())
                .put("foedselsaar", 2000);

        var personDataJson = objectMapper.createObjectNode();
        personDataJson.set("navn", objectMapper.createArrayNode().add(objectMapper.valueToTree(navn)));
        personDataJson.set("kjoenn", objectMapper.createArrayNode().add(objectMapper.valueToTree(kjoenn)));
        personDataJson.set("foedselsdato", objectMapper.createArrayNode().add(foedselsdato));

        var result = objectMapper.createObjectNode();
        result.set("data", objectMapper.createObjectNode().set("hentPerson", personDataJson));

        mockPdlResponse(objectMapper.writeValueAsString(result));

        return navn;
    }
}
