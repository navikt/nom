package no.nav.nom.apps.api.rest.dolly;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.app.NomRemedyIntegrasjonClient;
import no.nav.nom.apps.api.ressurs.api.app.OpprettRessursResponse;
import no.nav.nom.apps.api.ressurs.api.app.RessursContactDetailsResolver;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DollyControllerTest extends RestTestBase {

    @MockitoBean
    NIKClient nikClient;

    @MockitoBean
    PdlClient pdlClient;

    @MockitoBean
    NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;

    @MockitoBean
    RessursCommandService ressursCommandService;

    @MockitoBean
    OrgtilknytningCommandService orgtilknytningCommandService;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

    @MockitoBean
    OrgEnhetStateRepository orgEnhetStateRepository;

    @MockitoBean
    AuthResolver authResolver;

    @MockitoBean
    NomMastered nomMastered;

    @MockitoBean
    RessursContactDetailsResolver ressursContactDetailsResolver;

    @MockitoBean
    FysiskBrukerstedService fysiskBrukerstedService;

    @Autowired
    TestTokensGenerator testToken;

    @Autowired
    AppProperties appProperties;

    @BeforeEach
    void setMockExpectations(){
        when(authResolver.kanRedigereRessurs(any())).thenReturn(true);
        when(authResolver.kanOppretteRessurs(any())).thenReturn(true);
    }

    @Test
    @SneakyThrows
    void skal_opprette_statlig_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse();

        when(nikClient.opprettNavident(
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

        var request = new DollyRessursRequest(personident, navn.getFornavn(), navn.getMellomnavn(), navn.getEtternavn(), null, null);

        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(eq(List.of(appProperties.dollyRessurs().fysiskBrukerstedNr()))))
            .thenReturn(Set.of(appProperties.dollyRessurs().fysiskBrukerstedNr()));

        postOpprettRessursRequest(
            testToken.dollyClient(),
            request
        )
            .expectStatus().is2xxSuccessful()
            .expectBody(OpprettRessursResponse.class);
    }

    private WebTestClient.ResponseSpec postOpprettRessursRequest(String accessToken, DollyRessursRequest opprettRessursRequest) {
        return webTestClient.post()
            .uri("/api/v1/dolly/opprettRessurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(opprettRessursRequest)
            .exchange();
    }
}
