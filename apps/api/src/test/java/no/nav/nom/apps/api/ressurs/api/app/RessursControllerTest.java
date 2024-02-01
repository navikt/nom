package no.nav.nom.apps.api.ressurs.api.app;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static no.nav.nom.apps.api.ressurs.api.app.RessursController.UNLEASH_EDIT_RESSURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RessursControllerTest extends RestTestBase {

    @MockBean
    private NIKClient nikClient;

    @MockBean
    private PdlClient pdlClient;

    @MockBean
    private NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;

    @MockBean
    private RessursCommadService ressursCommadService;

    @MockBean
    private OrgtilknytningCommandService orgtilknytningCommandService;

    @MockBean
    private RessursStateRepository ressursStateRepository;

    @BeforeEach
    void enableFeatureFlag(){
        when(unleash.isEnabled(UNLEASH_EDIT_RESSURS)).thenReturn(true);
    }

    @Test
    @DisplayName("Should get ressurser that satisfies filter values")
    void test_getRessurser() {

        var sluttDatoFilterValue = LocalDate.now();
        var sektorFilterValue = Sektor.EKSTERN;

        when(ressursStateRepository.getAll()).thenReturn(List.of(
            ressursState().toBuilder()
                .sluttdato(Optional.of(sluttDatoFilterValue))
                .sektor(sektorFilterValue).build(),
            ressursState(),
            ressursState()
        ));

        var ressurser = webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser")
                .queryParam("sektor", Sektor.EKSTERN)
                .queryParam("sluttdato", LocalDate.now())
                .build()
            )
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomWriteUserToken()))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(RessursState.class)
            .returnResult().getResponseBody();

        assertThat(ressurser)
            .hasSize(1)
            .allMatch(ressursState -> ressursState.sektor().equals(sektorFilterValue))
            .allMatch(ressursState -> ressursState.sluttdato().equals(Optional.of(sluttDatoFilterValue)));


    }

    @ParameterizedTest(name = "Skal ikke opprette ressurs i sektor {0}")
    @MethodSource("skal_bare_opprette_eksterne_ressurser_input")
    void skal_bare_opprette_eksterne_ressurser(Sektor sektor) {

        webTestClient.post()
            .uri("/app/ressurser")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(opprettRessursRequest().toBuilder().sektor(sektor).build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(StandardResponse.class)
            .isEqualTo(StandardResponse.builder()
                .message(InvalidSektorException.actionNotAllowedForSektor(sektor).getMessage())
                .build()
            );
    }

    private static Stream<Arguments> skal_bare_opprette_eksterne_ressurser_input(){
        return Stream.of(
            Arguments.of(Sektor.NAV_STATLIG),
            Arguments.of(Sektor.NAV_KOMMUNAL)
        );
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
            "",
            Tilknytningtype.KONSULENT
        );

        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(eq(List.of(request.fysiskBrukerstedNr()))))
            .thenReturn(Set.of(request.fysiskBrukerstedNr()));

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
            "",
            Tilknytningtype.KONSULENT
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


    @ParameterizedTest(name = "Obfuskerer personident for respons i GET {0}")
    @MethodSource("getRessursPathsTestInput")
    void skalObfuskerePersonident(String endpointPath) {

        var personident = "01010154321";

        var ressursState = ressursState().toBuilder()
            .personident(personident).build();

        when(ressursStateRepository.findByNavident(anyString()))
            .thenReturn(Optional.of(ressursState));

        when(ressursStateRepository.findByFid(anyString()))
            .thenReturn(Optional.of(ressursState));

        webTestClient.get()
            .uri(endpointPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomWriteUserToken()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("personident")
            .isEqualTo(personident.replaceFirst(".{5}$", "*****"));

    }


    @ParameterizedTest(name = "Overstyrer obfuskering av personident for respons i GET {0}")
    @MethodSource("getRessursPathsTestInput")
    void skalOverstyreObfuskeringAvPersonident(String endpointPath) {

        var personident = "01010154321";

        var ressursState = ressursState().toBuilder()
            .personident(personident).build();

        when(ressursStateRepository.findByNavident(anyString()))
            .thenReturn(Optional.of(ressursState));

        when(ressursStateRepository.findByFid(anyString()))
            .thenReturn(Optional.of(ressursState));

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(endpointPath)
                .queryParam("obfuscatePersonident", false)
                .build()
            )
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomWriteUserToken()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("personident")
            .isEqualTo(personident);

    }

    static Stream<Arguments> getRessursPathsTestInput(){
        return Stream.of(
            Arguments.of("/app/ressurser/byNavident/whatever"),
            Arguments.of("/app/ressurser/byFid/whatever")
        );
    }


    @Test
    void skal_oppdatere_fysiske_brukersteder(){

        var fid = "FID-uuid";
        var existingRessurs = ressursState()
            .toBuilder()
            .fysiskeBrukersteder(null)
            .build();

        var updateFysiskeBrukerstederRequest = UpdateFysiskeBrukerstederRequest.builder()
            .primaryFysiskBrukersted("fyrstikken-id")
            .fysiskeBrukersteder(Set.of("fyrstikken-id"))
            .build();

        when(ressursStateRepository.findByFid(fid)).thenReturn(Optional.of(existingRessurs));
        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(argThat(l ->
                new HashSet<>(l).equals(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
            )
        )).thenReturn(updateFysiskeBrukerstederRequest.fysiskeBrukersteder());

        webTestClient.post()
            .uri(uriBuilder ->
                uriBuilder.path("/app/ressurser/{fid}/fysiskebrukersteder").build(fid)
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(updateFysiskeBrukerstederRequest)
            .exchange()
            .expectStatus().isOk();

        var oppdaterFysiskeBrukerstederCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
        verify(ressursCommadService, times(1))
            .oppdaterFysiskeBrukerstederCommand(oppdaterFysiskeBrukerstederCommandArgumentCaptor.capture(), any());

        assertThat(oppdaterFysiskeBrukerstederCommandArgumentCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(OppdaterFysiskeBrukerstederCommand.builder()
                .fid(existingRessurs.fid())
                .primaryFysiskBrukersted(updateFysiskeBrukerstederRequest.primaryFysiskBrukersted())
                .fysiskeBrukersteder(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
                .build()
            );
    }

    @ParameterizedTest(name = "updateFysiskeBrukersteder should give 400 with message '{1}'")
    @MethodSource("testUpdateFysiskeBrukerstederBadRequestInput")
    void testUpdateFysiskeBrukerstederBadRequest(UpdateFysiskeBrukerstederRequest updateFysiskeBrukerstederRequest, String expectedMessage){
        webTestClient.post()
            .uri("/app/ressurser/fid-whatever/fysiskebrukersteder")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(updateFysiskeBrukerstederRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(StandardResponse.class)
            .isEqualTo(StandardResponse.builder().message(expectedMessage).build());
    }

    static Stream<Arguments> testUpdateFysiskeBrukerstederBadRequestInput() {
        return Stream.of(
            Arguments.of(
                UpdateFysiskeBrukerstederRequest.builder()
                    .primaryFysiskBrukersted(null)
                    .fysiskeBrukersteder(emptySet())
                    .build(),
                "primaryFysiskBrukersted cannot be blank or null"
                ),
            Arguments.of(
                UpdateFysiskeBrukerstederRequest.builder()
                    .primaryFysiskBrukersted("oslo")
                    .fysiskeBrukersteder(Set.of("bergen"))
                    .build(),
                "Value for primaryFysiskBrukersted must be included in fysiske brukersteder"
            )
        );
    }

    OpprettRessursRequest opprettRessursRequest() {
        return OpprettRessursRequest.builder()
            .personident("whatever")
            .visningsfornavn("whatever")
            .visningsmellomnavn("whatever")
            .visningsetternavn("whatever")
            .sektor(Sektor.EKSTERN)
            .ansettelsesType(AnsettelsesType.FAST)
            .fysiskBrukerstedNr("whatever")
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();
    }
}
