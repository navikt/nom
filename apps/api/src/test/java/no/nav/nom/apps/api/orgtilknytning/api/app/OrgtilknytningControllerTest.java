package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgtilknytningControllerTest extends RestTestBase {

    @MockBean
    private OrgtilknytningCommandService orgtilknytningCommandService;

    @MockBean
    private RessursStateListener ressursStateListener;

    @Captor
    ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreOrgtilknytningDato> endreOrgtilknytningDatoArgumentCaptor;

    @BeforeEach
    void setInitialLoadComplete() {
        when(ressursStateListener.initloadComplete()).thenReturn(true);
        clearOrgtilknytninger();
        ressursStateRepository.clear();
    }

    @Test
    @DisplayName("Should get all orgtilknytninger when no query params specified")
    void test_get_all_orgtilknytninger() {
        var existingOrgtilknytninger = List.of(
            orgtilknytningState(),
            orgtilknytningState()
        );

        saveOrgtilknytninger(existingOrgtilknytninger);

        var responseBody =  webTestClient
            .get()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(OrgtilknytningState.class)
            .returnResult().getResponseBody();

        assertThat(responseBody)
            .hasSameElementsAs(existingOrgtilknytninger);
    }

    @Test
    @DisplayName("Should get all orgtilknytninger for navident")
    void test_get_all_orgtilknytninger_for_navident() {
        var navident = "Y654321";

        var orgtilknytningerForNavident = List.of(
            orgtilknytningState().toBuilder().navident(navident).build(),
            orgtilknytningState().toBuilder().navident(navident).build()
        );
        saveOrgtilknytninger(orgtilknytningerForNavident);

        var otherOrgtilknytninger = List.of(orgtilknytningState(), orgtilknytningState());
        saveOrgtilknytninger(otherOrgtilknytninger);

        var responseBody = webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger")
                .queryParam("navident", navident)
                .build()
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(OrgtilknytningState.class)
            .returnResult().getResponseBody();

        assertThat(responseBody)
            .hasSameElementsAs(orgtilknytningerForNavident);
    }

    @Test
    @DisplayName("Should update dato for orgtilknytning")
    void test_update_orgtilknytning_date() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreOrgtilknytningDatoRequest = new EndreOrgtilknytningDatoRequest(
            LocalDate.now(),
            Optional.of(LocalDate.now().plusYears(1))
        );

        when(unleash.isEnabled(any())).thenReturn(true);

        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build(orgtilknytningToUpdate.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(endreOrgtilknytningDatoRequest)
            .exchange()
            .expectStatus().is2xxSuccessful();

        verify(
            orgtilknytningCommandService,
            times(1)
        ).endreOrgtilknytningDato(
            stringArgumentCaptor.capture(),
            endreOrgtilknytningDatoArgumentCaptor.capture()
        );

        assertThat(stringArgumentCaptor.getValue()).isEqualTo(orgtilknytningToUpdate.fid());
        assertThat(endreOrgtilknytningDatoArgumentCaptor.getValue())
            .matches(endreOrgtilknytningDato ->
                endreOrgtilknytningDato.gyldigFom().equals(endreOrgtilknytningDatoRequest.gyldigFom())
            )
            .matches(endreOrgtilknytningDato ->
                endreOrgtilknytningDato.gyldigTom().equals(endreOrgtilknytningDatoRequest.gyldigTom())
            );
    }


    @Test
    @DisplayName("Should block update dato for orgtilknytning if tilknyttet ressurs is not EKSTERN")
    void test_update_orgtilknytning_for_ekstern_only_date() {

        when(unleash.isEnabled(any())).thenReturn(true);

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.NAV_STATLIG)
                .build()
        ));

        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build(orgtilknytningToUpdate.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(
                new EndreOrgtilknytningDatoRequest(
                    LocalDate.now(),
                    Optional.of(LocalDate.now().plusYears(1))
                )
            )
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should forbid update dato if user does not have granted authority 'MUTATE_RESSURS'")
    void test_update_orgtilknytning_forbidden() {

        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build("some-fid")
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .bodyValue(
                new EndreOrgtilknytningDatoRequest(
                    LocalDate.now(),
                    Optional.of(LocalDate.now().plusYears(1))
                )
            )
            .exchange()
            .expectStatus().isForbidden();
    }

}
