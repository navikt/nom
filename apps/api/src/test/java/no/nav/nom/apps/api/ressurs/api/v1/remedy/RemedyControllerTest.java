package no.nav.nom.apps.api.ressurs.api.v1.remedy;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RemedyControllerTest extends RestTestBase {

    @MockBean
    private NIKClient nikClient;

    @MockBean
    private RessursCommadService ressursCommadService;

    @MockBean
    protected RessursStateRepository ressursStateRepository;

    @Autowired
    TestTokensGenerator testToken;

    private Optional<RessursState> createRessursState(String personident, NavnDTO navn) {
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
            Optional.of(LocalDate.now().plusDays(10)),
            Optional.empty(),
            null,
            Collections.emptyList()
        );
        return Optional.of(ressursState);
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

        when(ressursStateRepository.findByPersonident(personident)).thenReturn(createRessursState(personident, navn));

        FinnRessursRequest request = new FinnRessursRequest();
        request.setPersonident(personident);

        RessursDTO expected = RessursDTO.builder()
            .status("200")
            .personident(personident)
            .navident("testNavIdent")
            .navn(navn)
            .build();

        webTestClient.post()
            .uri("/api/v1/remedy/finnRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.queryPersonClient());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(expected);
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
                httpHeaders.setBearerAuth(testToken.queryPersonClient());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(RessursDTO.builder().status("200").navn(expectedNavn).build());
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

        when(ressursStateRepository.findByPersonident(personident)).thenReturn(createRessursState(personident, navn));

        OpprettRessursRequest request = OpprettRessursRequest.builder()
            .personident(personident)
            .etternavn("X")
            .fornavn("Y")
            .build();

        RessursDTO expected = RessursDTO.builder()
            .status("200")
            .personident(personident)
            .navident("testNavIdent")
            .navn(navn)
            .build();

        webTestClient.post()
            .uri("/api/v1/remedy/opprettRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.mutateRessursClient());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(expected);
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
            .thenReturn(new RessursState(
                "fid",
                personident,
                navident,
                Sektor.EKSTERN,
                new Visningsnavn(navn.getFornavn(), Optional.of(navn.getMellomnavn()), navn.getEtternavn()),
                Optional.empty(), Optional.empty(),
                null,
                Collections.emptyList()
            ));

        OpprettRessursRequest request = OpprettRessursRequest.builder()
            .personident(personident)
            .etternavn(navn.getFornavn())
            .fornavn(navn.getEtternavn())
            .sektor(Sektor.EKSTERN.name())
            .build();

        RessursDTO expected = RessursDTO.builder()
            .status("200")
            .personident(personident)
            .navident(navident)
            .navn(navn)
            .build();

        webTestClient.post()
            .uri("/api/v1/remedy/opprettRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.mutateRessursClient());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(expected);

        Mockito.verify(nikClient).opprettNavident(
            eq(OpprettNavidentRequest.builder().personid(personident).build())
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Opprett ressurs som har hatt navident tidligere men som har byttet f.nr")
    void test_100() {
        String gml_personident = "12345678910";
        String ny_personident = "98765432100";
        String navident = "x123456";
        String eksisterendeFid = "FID_1";

        val navn = pdlUtils.mockSinglePersonResponse();

        when(nikClient.opprettNavident(
            eq(OpprettNavidentRequest.builder()
                .personid(ny_personident)
                .build())
        ))
            .thenReturn(ResponseEntity.ok(
                OpprettNavidentResponse.builder()
                    .navident(navident)
                    .build()));

        when(ressursStateRepository.findByPersonident(ny_personident)).thenReturn(Optional.empty()); // Nom har ikke ressurs på ny personident
        when(ressursStateRepository.findByNavident(navident)).thenReturn(Optional.of(new RessursState(
            // Men har eksisterende ressurs for navidenten returnert av nik
            eksisterendeFid,
            gml_personident,
            navident,
            Sektor.NAV_STATLIG,
            new Visningsnavn("Opprinnelig_fornavn", Optional.empty(), "Opprinnelig_etternavn"),
            Optional.empty(),
            Optional.empty(),
            null,
            Collections.emptyList()
        )));

        when(ressursCommadService.createRessursSync(any(OpprettRessursCommand.class)))
            .thenReturn(new RessursState(
                "fid",
                gml_personident,
                navident,
                Sektor.NAV_STATLIG,
                new Visningsnavn(navn.getFornavn(), Optional.of(navn.getMellomnavn()), navn.getEtternavn()),
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList()
            ));

        OpprettRessursRequest request = OpprettRessursRequest.builder()
            .personident(ny_personident)
            .fornavn(navn.getFornavn())
            .mellomnavn(navn.getMellomnavn())
            .etternavn(navn.getEtternavn())
            .sektor(Sektor.NAV_STATLIG.name())
            .build();

        RessursDTO expected = RessursDTO.builder()
            .personident(ny_personident)
            .navident(navident)
            .navn(navn)
            .build();

        webTestClient.post()
            .uri("/api/v1/remedy/opprettRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.mutateRessursClient());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(RessursDTO.class)
            .isEqualTo(expected);

        Mockito.verify(nikClient).opprettNavident(
            eq(OpprettNavidentRequest.builder().personid(ny_personident).build())
        );

        var expectedOppdaterPersonidentCommand = new OppdaterPersonidentCommand(eksisterendeFid, ny_personident);
        var expectedOppdaterPersonaliaCommand = new OppdaterPersonaliaCommand(
            eksisterendeFid,
            new Visningsnavn(navn.getFornavn(), Optional.of(navn.getMellomnavn()), navn.getEtternavn())
        );

        Mockito.verify(ressursCommadService).oppdaterPersonidentCommand(
            eq(expectedOppdaterPersonidentCommand), any()
        );
        Mockito.verify(ressursCommadService).oppdaterPersonaliaEvent(
            eq(expectedOppdaterPersonaliaCommand), any()
        );
    }
}
