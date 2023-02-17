package no.nav.nom.apps.api.rest.remedy;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class RemedyControllerTest extends RestTestBase {

    @Autowired
    private SecurityTestUtils securityTestUtils;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private NIKConsumer nikConsumer;

    @MockBean
    private RessursCommadService ressursCommadService;

    private void createRessursState(String personident, String fornavn, String mellomnavn, String etternavn) {
        RessursState ressursState = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            personident,
            "testNavIdent",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                fornavn,
                Optional.of(mellomnavn),
                etternavn
            ),
            Optional.of(LocalDate.now().plusDays(10))
        );
        createRessursHendelseEventSource(ressursState);
    }

    @Test
    @SneakyThrows
    void skal_hente_ressurs_fra_nom() {
        String personident = "testPersonIdent";
        String fornavn = "testFornavn";
        String mellomnavn = "testMellomnavn";
        String etternavn = "testEtternavn";
        createRessursState(personident, fornavn, mellomnavn, etternavn);

        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));

        val response = testRestTemplate.exchange(
            "/api/v1/remedy/ressurs/" + personident,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            RessursDTO.class
        );
        assertNotNull(response.getBody());

        Assertions.assertEquals(fornavn, response.getBody().getNavn().getFornavn());
        Assertions.assertEquals(mellomnavn, response.getBody().getNavn().getMellomnavn());
        Assertions.assertEquals(etternavn, response.getBody().getNavn().getEtternavn());
    }

    @Test
    @SneakyThrows
    void skal_hente_ressurs_fra_pdl() {
        String personident = "12345678910";
        val expectedNavn = pdlUtils.mockSinglePersonResponse(personident);

        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));

        val response = testRestTemplate.exchange(
            "/api/v1/remedy/ressurs/" + personident,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            RessursDTO.class
        );
        assertNotNull(response.getBody());

        Assertions.assertEquals(expectedNavn.getFornavn(), response.getBody().getNavn().getFornavn());
        Assertions.assertEquals(expectedNavn.getMellomnavn(), response.getBody().getNavn().getMellomnavn());
        Assertions.assertEquals(expectedNavn.getEtternavn(), response.getBody().getNavn().getEtternavn());
    }

    @Test
    @SneakyThrows
    void skal_hente_ressurs_fra_nom_ved_opprettelse_dersom_allerede_eksisterer() {
        String personident = "testPersonIdent";
        String fornavn = "testFornavn";
        String mellomnavn = "testMellomnavn";
        String etternavn = "testEtternavn";
        createRessursState(personident, fornavn, mellomnavn, etternavn);

        OpprettRessursRequest request = new OpprettRessursRequest();
        request.setPersonident(personident);
        request.setEtternavn("X");
        request.setFornavn("Y");

        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));

        val response = testRestTemplate.exchange(
            "/api/v1/remedy/ressurs/",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            RessursDTO.class
        );
        assertNotNull(response.getBody());

        Assertions.assertEquals(fornavn, response.getBody().getNavn().getFornavn());
        Assertions.assertEquals(mellomnavn, response.getBody().getNavn().getMellomnavn());
        Assertions.assertEquals(etternavn, response.getBody().getNavn().getEtternavn());
    }

    @Test
    @SneakyThrows
    void skal_opprette_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse(personident);

        when(nikConsumer.opprettNavident(eq(personident), isNull(), isNull(), eq(false)))
            .thenReturn(ResponseEntity.ok(
                OpprettNavidentResponse.builder()
                    .navident(navident)
                    .build()));

        when(ressursCommadService.createRessursSync(any(OpprettRessursCommand.class)))
            .thenReturn(new RessursState("fid", personident, navident,
                Sektor.EKSTERN,
                new Visningsnavn(navn.getFornavn(), Optional.of(navn.getMellomnavn()), navn.getEtternavn()),
                Optional.empty()
            ));

        OpprettRessursRequest request = new OpprettRessursRequest();
        request.setPersonident(personident);
        request.setEtternavn(navn.getFornavn());
        request.setFornavn(navn.getEtternavn());
        request.setSektor(Sektor.EKSTERN.name());

        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));

        val response = testRestTemplate.exchange(
            "/api/v1/remedy/ressurs/",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            RessursDTO.class
        );
        assertNotNull(response.getBody());

        Assertions.assertEquals(navn.getFornavn(), response.getBody().getNavn().getFornavn());
        Assertions.assertEquals(navn.getEtternavn(), response.getBody().getNavn().getEtternavn());

        Mockito.verify(nikConsumer).opprettNavident(eq(personident), isNull(), isNull(), eq(false));
    }

}
