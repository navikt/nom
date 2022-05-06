package no.nav.nom.apps.api.rest.person;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class PersonTest extends RestTestBase {

    @Autowired
    private SecurityTestUtils securityTestUtils;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL for person som er ressurs")
    void scenario1() {
        String personident = "12345678910";
        val expectedNavn = pdlUtils.mockSinglePersonResponse(personident);

        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));

        val response = testRestTemplate.exchange("/api/v1/person/getPerson", HttpMethod.POST, new HttpEntity<>(new PdlPersonRequest(personident), headers), PdlPersonDTO.class);
        Assertions.assertNotNull(response.getBody());

        Assertions.assertEquals(expectedNavn.getFornavn(), response.getBody().getNavn().getFornavn());
        Assertions.assertEquals(expectedNavn.getMellomnavn(), response.getBody().getNavn().getMellomnavn());
        Assertions.assertEquals(expectedNavn.getEtternavn(), response.getBody().getNavn().getEtternavn());
    }


    @Test
    @SneakyThrows
    @DisplayName("Ingen tilgang med ugyldig AZP")
    void scenario2() {
        String personident = "12345678910";

        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim().claim("azp", "invalid")));

        val response = testRestTemplate.exchange("/api/v1/person/getPerson", HttpMethod.POST, new HttpEntity<>(new PdlPersonRequest(personident), headers), String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Assertions.assertNull(response.getBody());
    }
}
