package no.nav.nom.apps.api.rest.remedy;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.SecurityTestUtils;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RemedyControllerTest extends RestTestBase {

    @Autowired
    private SecurityTestUtils securityTestUtils;

    @MockBean
    private NIKClient nikClient;

    @MockBean
    private RessursCommadService ressursCommadService;


    private void createRessursState(String personident, NavnDTO navn) {
        RessursState ressursState = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            personident,
            "testNavIdent",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                navn.getFornavn(),
                Optional.of(navn.getMellomnavn()),
                navn.getEtternavn()
            ),
            Optional.of(LocalDate.now().plusDays(10))
        );
        createRessursHendelseEventSource(ressursState);
    }

    @Test
    @SneakyThrows
    void skal_hente_ressurs_fra_nom() {
        String personident = "testPersonIdent";
        NavnDTO navn = NavnDTO.builder()
            .fornavn("testFornavn")
            .mellomnavn("testMellomnavn")
            .etternavn("testEtternavn")
            .build();
        createRessursState(personident, navn);

        FinnRessursRequest request = new FinnRessursRequest();
        request.setPersonident(personident);

        webTestClient.post()
            .uri("/api/v1/remedy/finnRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(RessursDTO.builder().navn(navn).build());
    }

    @Test
    @SneakyThrows
    void skal_hente_ressurs_fra_pdl() {
        String personident = "12345678910";
        val expectedNavn = pdlUtils.mockSinglePersonResponse();

        FinnRessursRequest request = new FinnRessursRequest();
        request.setPersonident(personident);

        webTestClient.post()
            .uri("/api/v1/remedy/finnRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(RessursDTO.builder().navn(expectedNavn).build());
    }

    @Test
    @SneakyThrows
    void skal_hente_ressurs_fra_nom_ved_opprettelse_dersom_allerede_eksisterer() {
        String personident = "testPersonIdent";

        NavnDTO navn = NavnDTO.builder()
            .fornavn("testFornavn")
            .mellomnavn("testMellomnavn")
            .etternavn("testEtternavn")
            .build();

        createRessursState(personident, navn);

        OpprettRessursRequest request = new OpprettRessursRequest();
        request.setPersonident(personident);
        request.setEtternavn("X");
        request.setFornavn("Y");

        webTestClient.post()
            .uri("/api/v1/remedy/opprettRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpClaim()));
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(RessursDTO.builder().navn(navn).build());
    }

    @Test
    @SneakyThrows
    void skal_opprette_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse();

        when(nikClient.opprettNavident(
            eq(OpprettNavidentRequest.builder()
                .personid(personident)
                .build())
        ))
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

        webTestClient.post()
            .uri("/api/v1/remedy/opprettRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpNameClaim()));
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(RessursDTO.builder()
                .navn(navn).build()
            );

        Mockito.verify(nikClient).opprettNavident(
            eq(OpprettNavidentRequest.builder().personid(personident).build())
        );
    }

    private JWTClaimsSet.Builder validJwtWithAzpNameClaim() {
        final var jwtClaimsBuilder = securityTestUtils.validJwtClaims();
        final var subject = jwtClaimsBuilder.getClaims().get("sub");
        securityTestUtils.validJwtClaims().claim("azp", subject);

        return securityTestUtils.validJwtClaims().claim("azp_name", subject);
    }

}
