package no.nav.nom.apps.api.orgenhet.api.app;

import no.nav.nom.apps.api.clients.koststed.OrgEnhetKostnadsstedMappingClient;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.AvsluttOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.EndreOpphengOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.EndreOpphengOrgEnhetResponse;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.ressurs.RessursService;
import no.nav.nom.apps.api.tilgangsenhet.TilgangsenhetRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrgEnhetRestServiceTest {

    @Mock
    private OrgEnhetIdGenerator orgEnhetIdGenerator;
    @Mock
    private OrgenhetService orgenhetService;
    @Mock
    private OrgEnhetMapper orgEnhetMapper;
    @Mock
    private RessursService ressursService;
    @Mock
    private OrgEnhetCommandService orgEnhetCommandService;
    @Mock
    private OrganiseringEventCommandService organiseringEventCommandService;
    @Mock
    private OrgEnhetLederCommandService orgEnhetLederCommandService;
    @Mock
    private OrganiseringStateRepository organiseringStateRepository;
    @Mock
    private TilgangsenhetRepository tilgangsenhetRepository;
    @Mock
    private OrgEnhetKostnadsstedMappingClient orgenhetKostnadsstedMappingClient;

    private OrgEnhetRestService service;

    @BeforeEach
    void setUp() {
        service = new OrgEnhetRestService(
            orgEnhetIdGenerator,
            orgenhetService,
            orgEnhetMapper,
            ressursService,
            orgEnhetCommandService,
            organiseringEventCommandService,
            orgEnhetLederCommandService,
            organiseringStateRepository,
            tilgangsenhetRepository,
            orgenhetKostnadsstedMappingClient
        );
        settInnloggetBruker();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("endreOpphengForOrgenhet - avslutter gammel organisering og oppretter ny")
    void endreOpphengForOrgenhet_oppretterNyttOppheng() {
        var gyldigFom = LocalDate.of(2026, 1, 10);
        var orgEnhetId = "123";
        var gammelOverordnetId = "200";
        var nyOverordnetId = "300";
        var gyldigTom = LocalDate.of(2099, 12, 31);

        var request = new EndreOpphengOrgEnhetRequest(
            orgEnhetId,
            gammelOverordnetId,
            gyldigFom,
            nyOverordnetId
        );

        doReturn(Optional.of(orgEnhet(orgEnhetId, "Enhet", LocalDate.of(2025, 1, 1), null)))
            .when(orgenhetService).getById(orgEnhetId);
        doReturn(Optional.of(orgEnhet(nyOverordnetId, "Ny overordnet", LocalDate.of(2020, 1, 1), null)))
            .when(orgenhetService).getById(nyOverordnetId);
        doReturn(Optional.of(OrganiseringState.builder()
            .fid("ORG-FID-1")
            .id(orgEnhetId)
            .idOver(gammelOverordnetId)
            .navn("Enhet")
            .navnOver("Gammel overordnet")
            .gyldigFom(LocalDate.of(2025, 1, 1))
            .gyldigTom(gyldigTom)
            .build()))
            .when(organiseringStateRepository).findActiveById(orgEnhetId);

        EndreOpphengOrgEnhetResponse response = service.endreOpphengForOrgenhet(request);

        ArgumentCaptor<AvsluttOrganiseringCmd> avsluttCaptor = ArgumentCaptor.forClass(AvsluttOrganiseringCmd.class);
        verify(organiseringEventCommandService).avsluttOrganisering(eq("ORG-FID-1"), avsluttCaptor.capture());
        assertEquals(gyldigFom.minusDays(1), avsluttCaptor.getValue().gyldigTom());

        ArgumentCaptor<OpprettOrganiseringCmd> opprettCaptor = ArgumentCaptor.forClass(OpprettOrganiseringCmd.class);
        verify(organiseringEventCommandService).opprettOrganisering(opprettCaptor.capture());
        assertEquals(orgEnhetId, opprettCaptor.getValue().id());
        assertEquals(nyOverordnetId, opprettCaptor.getValue().idOver());
        assertEquals(gyldigFom, opprettCaptor.getValue().gyldigFom());
        assertEquals(gyldigTom, opprettCaptor.getValue().gyldigTom());

        assertEquals(orgEnhetId, response.orgenhetId());
        assertEquals(nyOverordnetId, response.updatedOverenhetId());
        assertEquals(gyldigFom, response.gyldigFom());
        assertEquals(gyldigTom, response.gyldigTom());
    }

    @Test
    @DisplayName("endreOpphengForOrgenhet - kaster feil naar enheten allerede er knyttet til ny overordnet")
    void endreOpphengForOrgenhet_kasterNaarAlleredeKnyttet() {
        var request = new EndreOpphengOrgEnhetRequest("123", "200", LocalDate.of(2026, 1, 10), "200");

        doReturn(Optional.of(orgEnhet("123", "Enhet", LocalDate.of(2025, 1, 1), null)))
            .when(orgenhetService).getById("123");
        doReturn(Optional.of(orgEnhet("200", "Overordnet", LocalDate.of(2020, 1, 1), null)))
            .when(orgenhetService).getById("200");
        doReturn(Optional.of(OrganiseringState.builder()
            .fid("ORG-FID-1")
            .id("123")
            .idOver("200")
            .gyldigFom(LocalDate.of(2025, 1, 1))
            .build()))
            .when(organiseringStateRepository).findActiveById("123");

        assertThrows(IllegalStateException.class, () -> service.endreOpphengForOrgenhet(request));
        verify(organiseringEventCommandService, never()).avsluttOrganisering(anyString(), any());
        verify(organiseringEventCommandService, never()).opprettOrganisering(any());
    }

    @Test
    @DisplayName("endreOpphengForOrgenhet - kaster feil naar ny overordnet er avviklet foer opphengsdato")
    void endreOpphengForOrgenhet_kasterNaarNyOverordnetErAvviklet() {
        var request = new EndreOpphengOrgEnhetRequest("123", "200", LocalDate.of(2026, 1, 10), "300");

        doReturn(Optional.of(orgEnhet("123", "Enhet", LocalDate.of(2025, 1, 1), null)))
            .when(orgenhetService).getById("123");
        doReturn(Optional.of(orgEnhet("300", "Ny overordnet", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 9))))
            .when(orgenhetService).getById("300");
        doReturn(Optional.of(OrganiseringState.builder()
            .fid("ORG-FID-1")
            .id("123")
            .idOver("200")
            .gyldigFom(LocalDate.of(2025, 1, 1))
            .build()))
            .when(organiseringStateRepository).findActiveById("123");

        assertThrows(IllegalStateException.class, () -> service.endreOpphengForOrgenhet(request));
        verify(organiseringEventCommandService, never()).avsluttOrganisering(anyString(), any());
        verify(organiseringEventCommandService, never()).opprettOrganisering(any());
    }

    private OrgEnhet orgEnhet(String id, String navn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new OrgEnhet(OrgEnhetState.builder()
            .id(id)
            .navn(navn)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .build());
    }

    private void settInnloggetBruker() {
        var jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("Z999999")
            .claim("NAVident", "Z999999")
            .claim("azp_name", "test-app")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
