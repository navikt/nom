package no.nav.nom.apps.api.rest.backend.ressurs;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RessursControllerTest extends RestTestBase {

    @Autowired
    private SecurityTestUtils securityTestUtils;

    @MockBean
    private NIKClient nikClient;

    @MockBean
    private PdlClient pdlClient;

    @MockBean
    private RessursCommadService ressursCommadService;


    @Test
    @SneakyThrows
    void skal_opprette_ressurs() {
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

        when(ressursCommadService.createRessursSync(any(OpprettRessursCommand.class)))
            .thenReturn(
                new RessursState(
                    "fid",
                    personident,
                    navident,
                    Sektor.EKSTERN,
                    new Visningsnavn(navn.getFornavn(), Optional.of(navn.getMellomnavn()), navn.getEtternavn()),
                    Optional.empty()
                ));

        var request = new OpprettRessursRequest(
            personident,
            navn.getFornavn(),
            navn.getMellomnavn(),
            navn.getEtternavn(),
            Sektor.EKSTERN,
            "",
            LocalDate.now(),
            AnsettelsesType.FAST
        );

        webTestClient.post()
            .uri("/backend/ressurs")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(securityTestUtils.createToken(validJwtWithAzpNameClaim()));
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
                Sektor.EKSTERN
            ));
        ;
    }

    private JWTClaimsSet.Builder validJwtWithAzpNameClaim() {
        final var jwtClaimsBuilder = securityTestUtils.validJwtClaims();
        final var subject = jwtClaimsBuilder.getClaims().get("sub");
        jwtClaimsBuilder.claim("azp", subject);
        jwtClaimsBuilder.claim("NAVident", "NAVIDENT");
        jwtClaimsBuilder.claim("azp_name", subject);

        return jwtClaimsBuilder;
    }


}
