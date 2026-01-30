package no.nav.nom.apps.api.pdlutils;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@Component
@RequiredArgsConstructor
public class PdlUtils {
    private final AppProperties appProperties;
    private final JsonMapper jsonMapper;

    private void mockPdlResponse(String response) {
        stubFor(post("/graphql")
            .withHeader("Tema", equalTo(appProperties.clients().pdl().tema()))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    @SneakyThrows
    public NavnDTO mockSinglePersonResponse() {

        val navn = NavnDTO.builder().fornavn("fornavn").mellomnavn("mellomnavn").etternavn("etternavn").build();

        val kjoenn = KjonnDTO.builder().kjoenn(Kjonn.UKJENT).build();

        val foedselsdato = jsonMapper.createObjectNode()
                .put("foedselsdato", LocalDate.now().toString())
                .put("foedselsaar", 2000);

        var personDataJson = jsonMapper.createObjectNode();
        personDataJson.set("navn", jsonMapper.createArrayNode().add(jsonMapper.valueToTree(navn)));
        personDataJson.set("kjoenn", jsonMapper.createArrayNode().add(jsonMapper.valueToTree(kjoenn)));
        personDataJson.set("foedselsdato", jsonMapper.createArrayNode().add(foedselsdato));

        var result = jsonMapper.createObjectNode();
        result.set("data", jsonMapper.createObjectNode().set("hentPerson", personDataJson));

        mockPdlResponse(jsonMapper.writeValueAsString(result));

        return navn;
    }
}
