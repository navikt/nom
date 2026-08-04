package no.nav.nom.apps.api.orgenhetleder.api.app;

import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgenhetleder.api.app.dto.OpprettOrgEnhetLederRequest;
import no.nav.nom.apps.api.orgenhetleder.api.app.exception.OrgEnhetLederNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederSluttdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.SlettOrgEnhetLeder;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OrgEnhetLederRestServiceTest {

    @Mock
    private OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    @Mock
    private OrgEnhetLederCommandService orgEnhetLederCommandService;

    @Mock
    private OrgenhetService orgenhetService;

    @Mock
    private OrgtilknytningService orgtilknytningService;

    @Mock
    private OrganiseringStateRepository organiseringStateRepository;

    @Mock
    private RessursStateRepository ressursStateRepository;

    private OrgEnhetLederRestService orgEnhetLederRestService;

    private static final String ORG_ENHET_ID = "ab123c";
    private static final String ORG_ENHET_NAVN = "Utvikling 1A";
    private static final String NY_LEDER_NAVIDENT = "B000002";
    private static final String AKTIV_LEDER_FID = "FID-A";
    private static final String NY_LEDER_FID = "FID-NY";

    @BeforeEach
    void setUp() {
        var orgEnhetLederService = new OrgEnhetLederService(
            orgEnhetLederStateRepository,
            orgEnhetLederCommandService,
            orgenhetService,
            orgtilknytningService,
            organiseringStateRepository
        );
        orgEnhetLederRestService = new OrgEnhetLederRestService(
            orgEnhetLederService,
            ressursStateRepository,
            orgenhetService
        );
        settInnloggetBruker();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("opprettOrgEnhetLeder - aktiv leder uten sluttdato blir sluttdatert dagen foer ny leder overtar")
    void opprettOrgEnhetLeder_aktivLederUtenSluttdatoBlirSluttdatert() {
        // Given
        var gyldigFom = LocalDate.now().plusMonths(1);
        var request = new OpprettOrgEnhetLederRequest(ORG_ENHET_ID, NY_LEDER_NAVIDENT, gyldigFom, null);
        var aktivLeder = OrgEnhetLederState.builder()
            .fid(AKTIV_LEDER_FID).navident("A000001").orgEnhetId(ORG_ENHET_ID).startdato(LocalDate.now().minusYears(1)).build();

        // And
        doReturn(Optional.of(ressurs())).when(ressursStateRepository).findByNavident(NY_LEDER_NAVIDENT);
        doReturn(Optional.of(orgEnhet())).when(orgenhetService).getById(ORG_ENHET_ID);
        doReturn(Optional.of(aktivLeder)).when(orgEnhetLederStateRepository).getActiveForOrgEnhet(ORG_ENHET_ID);
        doReturn(Collections.emptyList()).when(orgEnhetLederStateRepository).findAllFutureByOrgEnhetId(ORG_ENHET_ID);
        doReturn(NY_LEDER_FID).when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any());

        // When
        var response = orgEnhetLederRestService.opprettOrgEnhetLeder(request);

        // Then
        ArgumentCaptor<OppdaterOrgEnhetLederSluttdato> sluttdatoCaptor = ArgumentCaptor.forClass(OppdaterOrgEnhetLederSluttdato.class);
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederSluttdato(eq(AKTIV_LEDER_FID), sluttdatoCaptor.capture());
        assertEquals(gyldigFom.minusDays(1), sluttdatoCaptor.getValue().sluttdato());

        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any());
        verify(orgEnhetLederCommandService, times(0)).slettOrgEnhetLederState(any());

        assertNotNull(response);
        assertEquals(ORG_ENHET_ID, response.orgEnhetId());
        assertEquals(ORG_ENHET_NAVN, response.orgEnhetNavn());
        assertEquals("Leder", response.visningsnavn().etternavn());
    }

    @Test
    @DisplayName("opprettOrgEnhetLeder - aktiv leder med arvet sluttdato blir forlenget til dagen foer ny leder overtar")
    void opprettOrgEnhetLeder_aktivLederMedArvetSluttdatoBlirForlenget() {
        // Given
        var gyldigFom = LocalDate.now().plusMonths(2);
        var request = new OpprettOrgEnhetLederRequest(ORG_ENHET_ID, NY_LEDER_NAVIDENT, gyldigFom, null);
        var aktivLederMedArvetSluttdato = OrgEnhetLederState.builder()
            .fid(AKTIV_LEDER_FID).navident("A000001").orgEnhetId(ORG_ENHET_ID)
            .startdato(LocalDate.now().minusYears(1)).sluttdato(LocalDate.now().plusDays(10)).build();

        // And
        doReturn(Optional.of(ressurs())).when(ressursStateRepository).findByNavident(NY_LEDER_NAVIDENT);
        doReturn(Optional.of(orgEnhet())).when(orgenhetService).getById(ORG_ENHET_ID);
        doReturn(Optional.of(aktivLederMedArvetSluttdato)).when(orgEnhetLederStateRepository).getActiveForOrgEnhet(ORG_ENHET_ID);
        doReturn(Collections.emptyList()).when(orgEnhetLederStateRepository).findAllFutureByOrgEnhetId(ORG_ENHET_ID);
        doReturn(NY_LEDER_FID).when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any());

        // When
        orgEnhetLederRestService.opprettOrgEnhetLeder(request);

        // Then - sluttdato blir forlenget til dagen foer ny leder, ikke beholdt paa den arvede datoen
        ArgumentCaptor<OppdaterOrgEnhetLederSluttdato> sluttdatoCaptor = ArgumentCaptor.forClass(OppdaterOrgEnhetLederSluttdato.class);
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederSluttdato(eq(AKTIV_LEDER_FID), sluttdatoCaptor.capture());
        assertEquals(gyldigFom.minusDays(1), sluttdatoCaptor.getValue().sluttdato());
    }

    @Test
    @DisplayName("opprettOrgEnhetLeder - eksisterende fremtidig leder blir slettet naar ny fremtidig leder settes")
    void opprettOrgEnhetLeder_eksisterendeFremtidigLederBlirSlettet() {
        // Given
        var gyldigFom = LocalDate.now().plusMonths(3);
        var request = new OpprettOrgEnhetLederRequest(ORG_ENHET_ID, NY_LEDER_NAVIDENT, gyldigFom, null);
        var aktivLeder = OrgEnhetLederState.builder()
            .fid(AKTIV_LEDER_FID).navident("A000001").orgEnhetId(ORG_ENHET_ID).startdato(LocalDate.now().minusYears(1)).build();
        var fremtidigLeder = OrgEnhetLederState.builder()
            .fid("FID-B").navident("C000003").orgEnhetId(ORG_ENHET_ID).startdato(LocalDate.now().plusMonths(1)).build();

        // And
        doReturn(Optional.of(ressurs())).when(ressursStateRepository).findByNavident(NY_LEDER_NAVIDENT);
        doReturn(Optional.of(orgEnhet())).when(orgenhetService).getById(ORG_ENHET_ID);
        doReturn(Optional.of(aktivLeder)).when(orgEnhetLederStateRepository).getActiveForOrgEnhet(ORG_ENHET_ID);
        doReturn(List.of(fremtidigLeder)).when(orgEnhetLederStateRepository).findAllFutureByOrgEnhetId(ORG_ENHET_ID);
        doReturn(NY_LEDER_FID).when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any());

        // When
        orgEnhetLederRestService.opprettOrgEnhetLeder(request);

        // Then - den fremtidige lederen blir slettet
        ArgumentCaptor<SlettOrgEnhetLeder> slettCaptor = ArgumentCaptor.forClass(SlettOrgEnhetLeder.class);
        verify(orgEnhetLederCommandService, times(1)).slettOrgEnhetLederState(slettCaptor.capture());
        assertEquals("FID-B", slettCaptor.getValue().fid());

        // Og aktiv leder blir sluttdatert dagen foer ny leder overtar
        ArgumentCaptor<OppdaterOrgEnhetLederSluttdato> sluttdatoCaptor = ArgumentCaptor.forClass(OppdaterOrgEnhetLederSluttdato.class);
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederSluttdato(eq(AKTIV_LEDER_FID), sluttdatoCaptor.capture());
        assertEquals(gyldigFom.minusDays(1), sluttdatoCaptor.getValue().sluttdato());

        // Og ny leder blir opprettet
        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any());
    }

    @Test
    @DisplayName("opprettOrgEnhetLeder - kaster RessursNotFoundException naar ny leder ikke finnes")
    void opprettOrgEnhetLeder_kasterNaarRessursIkkeFinnes() {
        // Given
        var request = new OpprettOrgEnhetLederRequest(ORG_ENHET_ID, NY_LEDER_NAVIDENT, LocalDate.now().plusMonths(1), null);

        // And
        doReturn(Optional.empty()).when(ressursStateRepository).findByNavident(NY_LEDER_NAVIDENT);

        // When / Then
        assertThrows(RessursNotFoundException.class, () -> orgEnhetLederRestService.opprettOrgEnhetLeder(request));
        verify(orgEnhetLederCommandService, times(0)).opprettOrgEnhetLeder(any());
    }

    @Test
    @DisplayName("opprettOrgEnhetLeder - kaster OrgEnhetNotFoundException naar orgenhet ikke finnes")
    void opprettOrgEnhetLeder_kasterNaarOrgEnhetIkkeFinnes() {
        // Given
        var request = new OpprettOrgEnhetLederRequest(ORG_ENHET_ID, NY_LEDER_NAVIDENT, LocalDate.now().plusMonths(1), null);

        // And
        doReturn(Optional.of(ressurs())).when(ressursStateRepository).findByNavident(NY_LEDER_NAVIDENT);
        doReturn(Optional.empty()).when(orgenhetService).getById(ORG_ENHET_ID);

        // When / Then
        assertThrows(OrgEnhetNotFoundException.class, () -> orgEnhetLederRestService.opprettOrgEnhetLeder(request));
        verify(orgEnhetLederCommandService, times(0)).opprettOrgEnhetLeder(any());
    }

    @Test
    @DisplayName("opprettOrgEnhetLeder - kaster OrgEnhetLederNotFoundException naar ingen aktiv leder finnes etter lagring")
    void opprettOrgEnhetLeder_kasterNaarIngenAktivLederEtterLagring() {
        // Given
        var request = new OpprettOrgEnhetLederRequest(ORG_ENHET_ID, NY_LEDER_NAVIDENT, LocalDate.now().plusMonths(1), null);

        // And
        doReturn(Optional.of(ressurs())).when(ressursStateRepository).findByNavident(NY_LEDER_NAVIDENT);
        doReturn(Optional.of(orgEnhet())).when(orgenhetService).getById(ORG_ENHET_ID);
        doReturn(Optional.empty()).when(orgEnhetLederStateRepository).getActiveForOrgEnhet(ORG_ENHET_ID);
        doReturn(Collections.emptyList()).when(orgEnhetLederStateRepository).findAllFutureByOrgEnhetId(ORG_ENHET_ID);
        doReturn(NY_LEDER_FID).when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any());

        // When / Then
        assertThrows(OrgEnhetLederNotFoundException.class, () -> orgEnhetLederRestService.opprettOrgEnhetLeder(request));
    }

    private RessursState ressurs() {
        return RessursState.builder()
            .navident(NY_LEDER_NAVIDENT)
            .visningsnavn(new Visningsnavn("Ny", Optional.empty(), "Leder"))
            .build();
    }

    private OrgEnhet orgEnhet() {
        return new OrgEnhet(OrgEnhetState.builder().id(ORG_ENHET_ID).navn(ORG_ENHET_NAVN).build());
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
