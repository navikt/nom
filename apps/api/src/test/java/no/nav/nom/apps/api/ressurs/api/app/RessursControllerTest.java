package no.nav.nom.apps.api.ressurs.api.app;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static no.nav.nom.apps.api.ressurs.api.app.RessursController.UNLEASH_EDIT_RESSURS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RessursControllerTest extends RestTestBase {

    @MockBean
    private NIKClient nikClient;

    @MockBean
    private PdlClient pdlClient;

    @MockBean
    private RessursCommadService ressursCommadService;

    @MockBean
    private OrgtilknytningCommandService orgtilknytningCommandService;

    @MockBean
    private RessursStateRepository ressursStateRepository;


    @Test
    @SneakyThrows
    void skal_opprette_statlig_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse();

        when(nikClient.opprettNavidentOnBehalfOfUser(
            eq(OpprettNavidentRequest.builder()
                .personid(personident)
                .identPrefix(navn.getEtternavn().substring(0, 1))
                .identType("STANDARD")
                .build())
        )).thenReturn(ResponseEntity.ok(
            OpprettNavidentResponse.builder()
                .navident(navident)
                .build()));

        when(pdlClient.hentPerson(eq(personident))).thenReturn(Optional.empty());

        var request = new OpprettRessursRequest(
            personident,
            navn.getFornavn(),
            navn.getMellomnavn(),
            navn.getEtternavn(),
            Sektor.NAV_STATLIG,
            "",
            LocalDate.now(),
            null,
            AnsettelsesType.FAST,
            ""
        );

        when(ressursCommadService.createRessursSync(any(OpprettRessursCommand.class)))
            .thenReturn(
                new RessursState(
                    "fid",
                    personident,
                    navident,
                    request.sektor(),
                    new Visningsnavn(navn.getFornavn(), Optional.of(navn.getMellomnavn()), navn.getEtternavn()),
                    request.sluttDato(),
                    Optional.empty(),
                    null,
                    Collections.emptyList()
                ));


        webTestClient.post()
            .uri("/app/ressurser")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(OpprettRessursResponse.class)
            .isEqualTo(new OpprettRessursResponse(
                personident,
                navident,
                navn.getFornavn() + " " + navn.getMellomnavn(),
                navn.getEtternavn(),
                request.sektor()
            ));
    }

    @Test
    @SneakyThrows
    void skal_opprette_ekstern_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse();

        when(nikClient.opprettNavidentOnBehalfOfUser(
            eq(OpprettNavidentRequest.builder()
                .personid(personident)
                .identPrefix(navn.getEtternavn().substring(0, 1))
                .identType("STANDARD")
                .build())
        )).thenReturn(ResponseEntity.ok(
            OpprettNavidentResponse.builder()
                .navident(navident)
                .build()));

        when(pdlClient.hentPerson(eq(personident))).thenReturn(Optional.empty());

        var request = new OpprettRessursRequest(
            personident,
            navn.getFornavn(),
            navn.getMellomnavn(),
            navn.getEtternavn(),
            Sektor.EKSTERN,
            "",
            LocalDate.now(),
            Optional.of(LocalDate.now()),
            null,
            ""
        );

        when(ressursCommadService.createRessursSync(any(OpprettRessursCommand.class)))
            .thenReturn(
                new RessursState(
                    "fid",
                    request.personident(),
                    navident,
                    request.sektor(),
                    new Visningsnavn(navn.getFornavn(), Optional.of(navn.getMellomnavn()), navn.getEtternavn()),
                    request.sluttDato(),
                    Optional.empty(),
                    null,
                    Collections.emptyList()
                ));


        webTestClient.post()
            .uri("/app/ressurser")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(OpprettRessursResponse.class)
            .isEqualTo(new OpprettRessursResponse(
                personident,
                navident,
                navn.getFornavn() + " " + navn.getMellomnavn(),
                navn.getEtternavn(),
                request.sektor()
            ));

        verify(ressursCommadService, times(1)).createRessursSync(any());
        verify(ressursCommadService, times(1)).avsluttRessursEvent(any(), any());

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(any(), any());
    }

    @Test
    @SneakyThrows
    void skal_ikke_opprette_ekstern_ressurs_uten_sluttDato() {
        String personident = "12345678910";
        val navn = pdlUtils.mockSinglePersonResponse();

        when(pdlClient.hentPerson(eq(personident))).thenReturn(Optional.empty());

        var request = new OpprettRessursRequest(
            personident,
            navn.getFornavn(),
            navn.getMellomnavn(),
            navn.getEtternavn(),
            Sektor.EKSTERN,
            "",
            LocalDate.now(),
            Optional.empty(),
            null,
            ""
        );

        webTestClient.post()
            .uri("/app/ressurser")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();


        verify(ressursCommadService, times(0)).createRessursSync(any());
        verify(ressursCommadService, times(0)).avsluttRessursEvent(any(), any());

        verify(orgtilknytningCommandService, times(0)).opprettOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(0)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).endreDagligOppfolgingOrgtilknytning(any(), any());
    }

    @Test
    void skal_kunne_endre_navn_for_eksterne() {
        var navident = "A123456";
        when(unleash.isEnabled(UNLEASH_EDIT_RESSURS)).thenReturn(true);
        when(ressursStateRepository.findByNavident(navident))
            .thenReturn(Optional.of(new RessursState(
                "FID_1",
                "01010154321",
                navident,
                Sektor.EKSTERN,
                null,
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList()
            )));


        var request = new UpdateVisningsnavnRequest(
            "Erik",
            Optional.of("Ten"),
            "Haag"
        );

        webTestClient.post()
            .uri("/app/ressurser/" + navident + "/navn")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk();

        verify(ressursCommadService, times(1)).oppdaterPersonaliaEvent(any(), any());
    }

    @Test
    void skal_kunne_avvikle_ressurs() {

        var ressurs = ressursState().toBuilder().fid("FID_1").navident("A123456").sektor(Sektor.EKSTERN).build();
        when(unleash.isEnabled(UNLEASH_EDIT_RESSURS)).thenReturn(true);
        when(ressursStateRepository.findByFid(ressurs.fid()))
            .thenReturn(Optional.of(ressurs));

        var existingOrgtilknytninger = List.of(
            orgtilknytningState().toBuilder().navident(ressurs.navident()).build(),
            orgtilknytningState().toBuilder()
                .navident(ressurs.navident())
                .gyldigTom(Optional.of(LocalDate.now().minusDays(1)))
                .build()
        );

        saveOrgtilknytninger(existingOrgtilknytninger);

        var request = new AvvikleRessursRequest(
            LocalDate.now().plusMonths(1)
        );

        webTestClient.post()
            .uri("/app/ressurser/" + ressurs.fid() + "/avvikle")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk();

        verify(ressursCommadService, times(1)).avsluttRessursEvent(any(), any());
        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), any());
    }

    @Test
    void skal_ikke_kunne_endre_navn_for_statlig() {
        var navident = "A123456";
        when(unleash.isEnabled(UNLEASH_EDIT_RESSURS)).thenReturn(true);
        when(ressursStateRepository.findByNavident(navident))
            .thenReturn(Optional.of(new RessursState(
                "FID_1",
                "01010154321",
                navident,
                Sektor.NAV_STATLIG,
                null,
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList()
            )));


        var request = new UpdateVisningsnavnRequest(
            "Erik",
            Optional.of("Ten"),
            "Haag"
        );

        webTestClient.post()
            .uri("/app/ressurser/" + navident + "/navn")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();

        verify(ressursCommadService, times(0)).oppdaterPersonaliaEvent(any(), any());
    }
}
