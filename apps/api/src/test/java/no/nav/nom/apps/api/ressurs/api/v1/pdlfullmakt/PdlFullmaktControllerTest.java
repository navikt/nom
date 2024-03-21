package no.nav.nom.apps.api.ressurs.api.v1.pdlfullmakt;

import lombok.SneakyThrows;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PdlFullmaktControllerTest extends RestTestBase {

    @MockBean
    protected RessursStateRepository ressursStateRepository;

    @Autowired
    TestTokensGenerator testToken;

    @Test
    @SneakyThrows
    void sjekk_av_personnummer_for_ikke_ansatt() {

        when(ressursStateRepository.findByPersonident(any())).thenReturn(Optional.empty());

        var request = new AnsattPersonRequest("01020354321");

        webTestClient.post()
            .uri("/api/v1/pdlfullmakt/getAnsatt")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.queryReprFullmaktClient());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(AnsattPersonResponse.class)
            .isEqualTo(new AnsattPersonResponse(null));
    }

    @Test
    @SneakyThrows
    void sjekk_av_personnummer_for_ansatt() {
        String personident = "01020354321";
        var startDato = LocalDate.now();
        var sluttDato = startDato.plusDays(10);

        when(ressursStateRepository.findByPersonident(personident)).thenReturn(
            createRessursState(personident,startDato,sluttDato));


        var request = new AnsattPersonRequest(personident);

        webTestClient.post()
            .uri("/api/v1/pdlfullmakt/getAnsatt")

            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.queryReprFullmaktClient());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(AnsattPersonResponse.class)
            .isEqualTo(new AnsattPersonResponse(new AnsattPersonResponse.AnsattPerson("testNavIdent",startDato.toString(),sluttDato.toString(),Sektor.NAV_STATLIG)));


    }

    private Optional<RessursState> createRessursState(String personident, LocalDate start, LocalDate slutt) {
        RessursState ressursState = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            personident,
            "testNavIdent",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "test",
                Optional.of("kanskje"),
                "etter"
            ),
            Optional.of(slutt),
            Optional.of(start),
            null,
            Collections.emptyList()
        );
        return Optional.of(ressursState);
    }

}
