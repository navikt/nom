package no.nav.nom.apps.api.orgenhetleder;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhetleder.api.app.OrgEnhetLederService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OrgEnhetLederServiceTest {

    @Mock
    private OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    @Mock
    private Unleash unleash;

    @Mock
    private OrgEnhetLederCommandService orgEnhetLederCommandService;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private OrgenhetService orgenhetService;

    @InjectMocks
    private OrgEnhetLederService orgEnhetLederService;

    @BeforeEach
    void setUp() {
        doReturn(true).when(leaderElection).isLeader();
        doReturn(true).when(unleash).isEnabled(any());
    }

    @Test
    @DisplayName("Test at ingen leder er persistert i state for orgenhet og en Opprettet og OppdatertStartdato blir sendt")
    void test_001() {
        // Given
        var orgEnhetId = "ab123c";
        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000001").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1A").build()
        );

        // And
        doReturn(Collections.emptyList()).when(orgEnhetLederStateRepository).findByOrgEnhetId(orgEnhetId);
        doReturn("FID-001").when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetId).build()))).when(orgenhetService).getEnhetByAgressoIdNivaAndName(dvhLeder.getFirst().getOrgEnhetNr(), dvhLeder.getFirst().getOrgNiv(), dvhLeder.getFirst().getOrgenhetNavn());

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then
        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(0)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }

    @Test
    @DisplayName("Test at info som kommer fra DVH er lik den i NOM og ingenting skjer")
    void test_002() {
        // Given
        var orgEnhetId = "ab123c";
        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000001").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1A").build()
        );
        var nomLeder = List.of(
            OrgEnhetLederState.builder().fid("FID-001").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now()).build()
        );

        // And
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetId).build()))).when(orgenhetService).getEnhetByAgressoIdNivaAndName(dvhLeder.getFirst().getOrgEnhetNr(), dvhLeder.getFirst().getOrgNiv(), dvhLeder.getFirst().getOrgenhetNavn());
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findByOrgEnhetId(orgEnhetId);

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then

        verify(orgEnhetLederCommandService, times(0)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(0)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(0)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }

    @Test
    @DisplayName("Test at info som kommer fra DVH er annerledes enn den i NOM og endring skjer")
    void test_003() {
        // Given
        var orgEnhetId = "ab123c";
        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000002").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1A").build()
        );
        var nomLeder = List.of(
            OrgEnhetLederState.builder().fid("FID-001").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now()).build()
        );

        // And
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findByOrgEnhetId(orgEnhetId);
        doReturn("FID-002").when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetId).build()))).when(orgenhetService).getEnhetByAgressoIdNivaAndName(dvhLeder.getFirst().getOrgEnhetNr(), dvhLeder.getFirst().getOrgNiv(), dvhLeder.getFirst().getOrgenhetNavn());

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then

        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }

    @Test
    @DisplayName("Test at det finnes flere ledere som er aktive og bare en matcher den fra DVH")
    void test_004() {
        // Given
        var orgEnhetId = "ab123c";
        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000003").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1A").build()
        );
        var nomLeder = List.of(
            OrgEnhetLederState.builder().fid("FID-001").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now()).build(),
            OrgEnhetLederState.builder().fid("FID-002").navident("A000002").orgEnhetId(orgEnhetId).startdato(LocalDate.now()).build(),
            OrgEnhetLederState.builder().fid("FID-003").navident("A000003").orgEnhetId(orgEnhetId).startdato(LocalDate.now()).build()
        );

        // And
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findByOrgEnhetId(orgEnhetId);
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetId).build()))).when(orgenhetService).getEnhetByAgressoIdNivaAndName(dvhLeder.getFirst().getOrgEnhetNr(), dvhLeder.getFirst().getOrgNiv(), dvhLeder.getFirst().getOrgenhetNavn());

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then
        verify(orgEnhetLederCommandService, times(0)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(0)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(2)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }

    @Test
    @DisplayName("Test at ingen aktive ledere finnes i NOM og at den fra DVH blir opprettet")
    void test_005() {
        // Given
        var orgEnhetId = "ab123c";
        var localDateNow = LocalDate.now();

        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000004").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1A").build()
        );

        var nomLeder = List.of(
            OrgEnhetLederState.builder().fid("FID-001").navident("A000001").orgEnhetId(orgEnhetId).startdato(localDateNow.minusYears(2)).sluttdato(localDateNow.minusYears(1)).build(),
            OrgEnhetLederState.builder().fid("FID-002").navident("A000002").orgEnhetId(orgEnhetId).startdato(localDateNow.minusYears(1)).sluttdato(localDateNow.minusMonths(6)).build(),
            OrgEnhetLederState.builder().fid("FID-003").navident("A000003").orgEnhetId(orgEnhetId).startdato(localDateNow.minusMonths(6)).sluttdato(localDateNow.minusDays(1)).build()
        );

        // And
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findByOrgEnhetId(orgEnhetId);
        doReturn("FID-004").when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetId).build()))).when(orgenhetService).getEnhetByAgressoIdNivaAndName(dvhLeder.getFirst().getOrgEnhetNr(), dvhLeder.getFirst().getOrgNiv(), dvhLeder.getFirst().getOrgenhetNavn());

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then
        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(0)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }
}
