package no.nav.nom.apps.api.orgenhetleder;

import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private OrgEnhetLederCommandService orgEnhetLederCommandService;

    @Mock
    private OrgenhetService orgenhetService;

    @Mock
    private OrgtilknytningService orgtilknytningService;

    @InjectMocks
    private OrgEnhetLederService orgEnhetLederService;

    @Mock
    OrganiseringStateRepository organiseringStateRepository;

    @Test
    @DisplayName("Test at ingen leder er persistert i state for orgenhet og en Opprettet og OppdatertStartdato blir sendt")
    void test_001() {
        // Given
        var orgEnhetId = "ab123c";
        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000001").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1A").build()
        );

        // And
        doReturn(Collections.emptyList()).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(orgEnhetId);
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
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(orgEnhetId);

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
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(orgEnhetId);
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
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(orgEnhetId);
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
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(orgEnhetId);
        doReturn("FID-004").when(orgEnhetLederCommandService).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetId).build()))).when(orgenhetService).getEnhetByAgressoIdNivaAndName(dvhLeder.getFirst().getOrgEnhetNr(), dvhLeder.getFirst().getOrgNiv(), dvhLeder.getFirst().getOrgenhetNavn());

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then
        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(0)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }

    @Test
    @DisplayName("Test at det finnes flere ledere og er samme leder som er aktive og bare en matcher den fra DVH")
    void test_006() {
        // Given
        var orgEnhetId = "ab123c";
        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000001").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 3B").build()
        );
        var nomLeder = List.of(
            OrgEnhetLederState.builder().fid("FID-001").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now().minusMonths(1)).sluttdato(LocalDate.now().minusDays(15)).build(),
            OrgEnhetLederState.builder().fid("FID-002").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now()).build(),
            OrgEnhetLederState.builder().fid("FID-003").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now().minusMonths(1)).sluttdato(LocalDate.now().minusMonths(1).minusDays(1)).build(),
            OrgEnhetLederState.builder().fid("FID-004").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now().minusMonths(1)).sluttdato(LocalDate.now().minusMonths(1)).build(),
            OrgEnhetLederState.builder().fid("FID-005").navident("A000001").orgEnhetId(orgEnhetId).startdato(LocalDate.now()).build()
        );

        // And
        doReturn(nomLeder).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(orgEnhetId);
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetId).build()))).when(orgenhetService).getEnhetByAgressoIdNivaAndName(dvhLeder.getFirst().getOrgEnhetNr(), dvhLeder.getFirst().getOrgNiv(), dvhLeder.getFirst().getOrgenhetNavn());

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then
        verify(orgEnhetLederCommandService, times(0)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(0)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }

    @Test
    @DisplayName("get ledere for Ressurs - Returns empty list when no orgtilknytning exists for navident - kunDOL = false")
    void getLedereForRessurs_noOrgtilknytningForNavident() {
        // Given
        String navident = "A000001";
        doReturn(Collections.emptyList()).when(orgtilknytningService).getByNavIdent(navident);

        // When
        var result = orgEnhetLederService.getLedereForRessurs(navident, false);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get ledere for Ressurs - Returns empty list when no orgtilknytning exists for navident - kunDOL = true")
    void getLedereForRessurs_noOrgtilknytningForNavident_DOL_true() {
        // Given
        String navident = "A000001";
        doReturn(Collections.emptyList()).when(orgtilknytningService).getByNavIdent(navident);

        // When
        var result = orgEnhetLederService.getLedereForRessurs(navident, true);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get ledere for Ressurs - Filters orgtilknytning to only DOL")
    void getLedereForRessurs_filtersWhenKunDOLIsTrue() {
        // Given
        String navident = "A000001";
        String leder1001 = "B001001";

        var otStartDate = LocalDate.now().minusDays(10);
        var otEndDate = LocalDate.now().plusDays(10);

        var orgtilknytning = List.of(
            OrgtilknytningState.builder().orgEnhetId("1001").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(true).build(),
            OrgtilknytningState.builder().orgEnhetId("1002").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(false).build()
        );
        doReturn(orgtilknytning).when(orgtilknytningService).getByNavIdent(navident);

        var lederStates1001 = List.of(OrgEnhetLederState.builder().navident(leder1001).orgEnhetId("1001").startdato(LocalDate.now().minusDays(20)).sluttdato(null).build());

        doReturn(lederStates1001).when(orgEnhetLederStateRepository).findAllByOrgEnhetId("1001");

        // When
        var result = orgEnhetLederService.getLedereForRessurs(navident, true);

        // Then
        assertEquals(1, result.size());
        assertEquals("1001", result.getFirst().orgEnhetId());
        assertEquals(leder1001, result.getFirst().lederNavident());
        assertEquals(otStartDate, result.getFirst().startdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertTrue(result.getFirst().erDagligOppfolging());
    }

    @Test
    @DisplayName("get ledere for Ressurs - Filters orgtilknytning to not only DOL")
    void getLedereForRessurs_filtersWhenKunDOLIsFalse() {
        // Given
        String navident = "A000001";
        String leder1001 = "B001001";
        String leder1002 = "B001002";

        var otStartDate = LocalDate.now().minusDays(10);
        var otEndDate = LocalDate.now().plusDays(10);

        var orgtilknytning = List.of(
            OrgtilknytningState.builder().orgEnhetId("1001").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(true).build(),
            OrgtilknytningState.builder().orgEnhetId("1002").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(false).build()
        );
        doReturn(orgtilknytning).when(orgtilknytningService).getByNavIdent(navident);

        var lederStates1001 = List.of(OrgEnhetLederState.builder().navident(leder1001).orgEnhetId("1001").startdato(LocalDate.now().minusDays(20)).sluttdato(null).build());
        var lederStates1002 = List.of(OrgEnhetLederState.builder().navident(leder1002).orgEnhetId("1002").startdato(LocalDate.now().minusDays(20)).sluttdato(null).build());

        doReturn(lederStates1001).when(orgEnhetLederStateRepository).findAllByOrgEnhetId("1001");
        doReturn(lederStates1002).when(orgEnhetLederStateRepository).findAllByOrgEnhetId("1002");

        // When
        var result = orgEnhetLederService.getLedereForRessurs(navident, false);

        // Then
        assertEquals(2, result.size());
        assertEquals("1001", result.getFirst().orgEnhetId());
        assertEquals(leder1001, result.getFirst().lederNavident());
        assertEquals(otStartDate, result.getFirst().startdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertTrue(result.getFirst().erDagligOppfolging());

        assertEquals("1002", result.getLast().orgEnhetId());
        assertEquals(leder1002, result.getLast().lederNavident());
        assertFalse(result.getLast().erDagligOppfolging());
    }

    @Test
    @DisplayName("get ledere for Ressurs - Includes leaders from over-enheter when navident matches")
    void getLedereForRessurs_includesLeadersFromOverEnheterWhenNavidentMatches() {
        // Given
        String navident = "A000001";
        String leder10 = "B000010";
        String enhet1001    = "1001";
        String overEnhet100 = "100";
        String overEnhet10  = "10";

        var otStartDate = LocalDate.now().minusDays(10);
        var otEndDate = LocalDate.now().plusDays(10);

        var orgtilknytning = List.of(
            OrgtilknytningState.builder().orgEnhetId(enhet1001).gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(true).build()
        );
        var lederState1001  = OrgEnhetLederState.builder().navident(navident).orgEnhetId(enhet1001).startdato(otStartDate).sluttdato(otEndDate).build();
        var lederState100   = OrgEnhetLederState.builder().navident(navident).orgEnhetId(overEnhet100).startdato(otStartDate).sluttdato(otEndDate).build();
        var lederState10    = OrgEnhetLederState.builder().navident(leder10).orgEnhetId(overEnhet10).startdato(otStartDate).sluttdato(otEndDate).build();

        doReturn(orgtilknytning).when(orgtilknytningService).getByNavIdent(navident);
        doReturn(List.of(lederState1001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet1001);
        doReturn(List.of(lederState100)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet100);
        doReturn(List.of(lederState10)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet10);

        var organiseringStateForId1001 = List.of(
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100).navnOver("Enhet-"+overEnhet100).gyldigFom(otStartDate.minusYears(1)).build()
        );
        var organiseringStateForId100 = List.of(
            OrganiseringState.builder().id(overEnhet100).navn("Enhet-"+overEnhet100).idOver(overEnhet10).navnOver("Enhet-"+overEnhet10).gyldigFom(otStartDate.minusYears(1)).build(),
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100).navnOver("Enhet-"+overEnhet100).gyldigFom(otStartDate.minusYears(1)).build()
        );
        doReturn(organiseringStateForId1001).when(organiseringStateRepository).findById(enhet1001);
        doReturn(organiseringStateForId100).when(organiseringStateRepository).findById(overEnhet100);

        // When
        var result = orgEnhetLederService.getLedereForRessurs(navident, false);

        // Then
        assertEquals(1, result.size());
        assertEquals("10", result.getFirst().orgEnhetId());
        assertEquals(leder10, result.getFirst().lederNavident());
        assertEquals(otStartDate, result.getFirst().startdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertTrue(result.getFirst().erDagligOppfolging());
    }

    @Test
    @DisplayName("get ledere for Ressurs - Handles different complex cases with different periods")
    void getLedereForRessurs_handlesOverlappingPeriodsForOverEnheter() {
        // Given
        String navident = "A000001";

        String leder1001Before = "B001001";
        String leder2001 = "B002001";
        String leder3001 = "B003001";
        String leder4001 = "B004001";
        String leder10A = "B000010A";
        String leder10B1 = "B000010B1";
        String leder10B2 = "B000010B2";

        String enhet2001    = "2001";
        String enhet3001    = "3001";
        String enhet4001    = "4001";

        String enhet1001    = "1001";
        String overEnhet100A = "100A";
        String overEnhet100B = "100B";
        String overEnhet10A  = "10A";
        String overEnhet10B  = "10B";

        var d1InMonth3YAgo = LocalDate.now().minusYears(3).withDayOfMonth(1);
        var lastDayInMonth2YAgo = LocalDate.now().minusYears(2).withDayOfMonth(1).minusDays(1);
        var d1InMonth1YAgo = LocalDate.now().minusYears(1).withDayOfMonth(1);
        var d1InMonth8MAgo = LocalDate.now().minusMonths(8).withDayOfMonth(1);
        var d15InMonth6MAgo = LocalDate.now().minusMonths(6).withDayOfMonth(15);
        var d16InMonth6MAgo =  LocalDate.now().minusMonths(6).withDayOfMonth(16);
        var d15InMonth3MAgo = LocalDate.now().minusMonths(3).withDayOfMonth(15);
        var d16InMonth3MAgo =  LocalDate.now().minusMonths(3).withDayOfMonth(16);
        var lastDayInMonth4MAgo = LocalDate.now().minusMonths(3).withDayOfMonth(1).minusDays(1);
        var d1InMonth3MAgo = LocalDate.now().minusMonths(3).withDayOfMonth(1);

        // Orgtilknytninger

        //           3YAgo                2YAgo              1YAgo               8MAgo               6M+15d Ago                   3MAgo    3M+15d Ago
        //             |                    |                  |                   |                     |                          |          |
        //-------------><-------------------><-----------------><------------------><--------------------><-------------------------><---------><--------------
        //                                                      <------------ 1001 DOL ------------------------------------------------------------------------
        //                                                                          <------ 2001 ----------------------------------->
        //                                                      <------ 3001 ------------------------------------------------------->
        //              <---- 4001 DOL ----->
        var orgtilknytninger = List.of(
            OrgtilknytningState.builder().orgEnhetId(enhet1001).gyldigFom(d1InMonth1YAgo).gyldigTom(null).erDagligOppfolging(true).build(),
            OrgtilknytningState.builder().orgEnhetId(enhet2001).gyldigFom(d1InMonth8MAgo).gyldigTom(lastDayInMonth4MAgo).erDagligOppfolging(false).build(),
            OrgtilknytningState.builder().orgEnhetId(enhet3001).gyldigFom(d1InMonth1YAgo).gyldigTom(lastDayInMonth4MAgo).erDagligOppfolging(false).build(),
            OrgtilknytningState.builder().orgEnhetId(enhet4001).gyldigFom(d1InMonth3YAgo).gyldigTom(lastDayInMonth2YAgo).erDagligOppfolging(true).build()
        );
        doReturn(orgtilknytninger).when(orgtilknytningService).getByNavIdent(navident);

        // Ledere
        //             3YAgo                2YAgo              1YAgo               8MAgo             6M+15d Ago                   3MAgo    3M+15d Ago
        //               |                    |                  |                   |                   |                          |          |
        // Enhet  -------><-------------------><-----------------><------------------><------------------><-------------------------><---------><--------------
        //  2001          <---- leder2001 ---------------------------------------------------------------------------------------------------------------------
        //  3001          <---- leder3001 ---------------------------------------------------------------------------------------------------------------------
        //  4001          <---- leder4001 ---------------------------------------------------------------------------------------------------------------------
        //  1001          <---- leder1001Before ---------------------------------------------------------><-------- navident ----------------------------------
        //  100A                                                                                          <-------- navident ----------------------------------
        //  100B                                                                                          <-------- navident ----------------------------------
        //  10A                                                   <------------------------------------------------ leder10A ----------------------------------
        //  10B                                                   <------------------------------------------------ leder10B1 -----------------><--- leder10B2 -

        var lederState2001  = OrgEnhetLederState.builder().navident(leder2001).orgEnhetId(enhet2001).startdato(d1InMonth3YAgo).sluttdato(null).build();
        var lederState3001  = OrgEnhetLederState.builder().navident(leder3001).orgEnhetId(enhet3001).startdato(d1InMonth3YAgo).sluttdato(null).build();
        var lederState4001  = OrgEnhetLederState.builder().navident(leder4001).orgEnhetId(enhet4001).startdato(d1InMonth3YAgo).sluttdato(null).build();

        var lederState1001Before  = OrgEnhetLederState.builder().navident(leder1001Before).orgEnhetId(enhet1001).startdato(d1InMonth3YAgo).sluttdato(d15InMonth6MAgo).build();
        var lederState1001Now  = OrgEnhetLederState.builder().navident(navident).orgEnhetId(enhet1001).startdato(d16InMonth6MAgo).sluttdato(null).build();

        var lederState100A   = OrgEnhetLederState.builder().navident(navident).orgEnhetId(overEnhet100A).startdato(d16InMonth6MAgo).sluttdato(null).build();
        var lederState100B    = OrgEnhetLederState.builder().navident(navident).orgEnhetId(overEnhet100B).startdato(d16InMonth6MAgo).sluttdato(null).build();

        var lederState10A   = OrgEnhetLederState.builder().navident(leder10A).orgEnhetId(overEnhet10A).startdato(d1InMonth1YAgo).sluttdato(null).build();

        var lederState10B1    = OrgEnhetLederState.builder().navident(leder10B1).orgEnhetId(overEnhet10B).startdato(d1InMonth1YAgo).sluttdato(d15InMonth3MAgo).build();
        var lederState10B2    = OrgEnhetLederState.builder().navident(leder10B2).orgEnhetId(overEnhet10B).startdato(d16InMonth3MAgo).sluttdato(null).build();

        doReturn(List.of(lederState2001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet2001);
        doReturn(List.of(lederState3001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet3001);
        doReturn(List.of(lederState4001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet4001);

        doReturn(List.of(lederState1001Before, lederState1001Now)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet1001);

        doReturn(List.of(lederState100A)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet100A);
        doReturn(List.of(lederState100B)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet100B);
        doReturn(List.of(lederState10A)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet10A);
        doReturn(List.of(lederState10B1, lederState10B2)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet10B);

        // Organisering/hierarki
        //             3YAgo                2YAgo              1YAgo               8MAgo             6M+15d Ago                   3MAgo    3M+15d Ago
        //               |                    |                  |                   |                   |                          |          |
        // Enhet  -------><-------------------><-----------------><------------------><------------------><-------------------------><---------><--------------
        //  1001          <--------------------------------------------- 100A ------------------------------------------------------><---- 100B ---------------
        //  100A          <---------------------------------------------- 10A ---------------------------------------------------------------------------------
        //  100B          <---------------------------------------------- 10B ---------------------------------------------------------------------------------
        var organiseringStateForId1001 = List.of(
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100A).navnOver("Enhet-"+overEnhet100A).gyldigFom(d1InMonth3YAgo).gyldigTom(lastDayInMonth4MAgo).build(),
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100B).navnOver("Enhet-"+overEnhet100B).gyldigFom(d1InMonth3MAgo).build()
        );
        var organiseringStateForId100A = List.of(
            OrganiseringState.builder().id(overEnhet100A).navn("Enhet-"+overEnhet100A).idOver(overEnhet10A).navnOver("Enhet-"+overEnhet10A).gyldigFom(d1InMonth3YAgo).gyldigTom(lastDayInMonth4MAgo).build()
        );
        var organiseringStateForId100B = List.of(
            OrganiseringState.builder().id(overEnhet100B).navn("Enhet-"+overEnhet100B).idOver(overEnhet10B).navnOver("Enhet-"+overEnhet10B).gyldigFom(d1InMonth3YAgo).build()
        );

        doReturn(organiseringStateForId1001).when(organiseringStateRepository).findById(enhet1001);
        doReturn(organiseringStateForId100A).when(organiseringStateRepository).findById(overEnhet100A);
        doReturn(organiseringStateForId100B).when(organiseringStateRepository).findById(overEnhet100B);

        // When
        var result = orgEnhetLederService.getLedereForRessurs(navident, false);

        // Then
        assertEquals(7, result.size());

        var lederRessurs4001 = result.getFirst();
        var lederRessurs1001Before = result.get(1);

        var lederRessurs3001 = result.get(2);
        var lederRessurs2001 = result.get(3);

        var lederRessurs1001After10A = result.get(4);
        var lederRessurs1001After10B1 = result.get(5);
        var lederRessurs1001After10B2 = result.get(6);

        assertEquals(enhet4001,             lederRessurs4001.orgEnhetId());
        assertEquals(leder4001,             lederRessurs4001.lederNavident());
        assertEquals(d1InMonth3YAgo,        lederRessurs4001.startdato());
        assertEquals(lastDayInMonth2YAgo,   lederRessurs4001.sluttdato());
        assertTrue(                         lederRessurs4001.erDagligOppfolging());

        assertEquals(enhet1001,             lederRessurs1001Before.orgEnhetId());
        assertEquals(leder1001Before,       lederRessurs1001Before.lederNavident());
        assertEquals(d1InMonth1YAgo,        lederRessurs1001Before.startdato());
        assertEquals(d15InMonth6MAgo,       lederRessurs1001Before.sluttdato());
        assertTrue(                         lederRessurs1001Before.erDagligOppfolging());

        assertEquals(enhet3001,             lederRessurs3001.orgEnhetId());
        assertEquals(leder3001,             lederRessurs3001.lederNavident());
        assertEquals(d1InMonth1YAgo,        lederRessurs3001.startdato());
        assertEquals(lastDayInMonth4MAgo,   lederRessurs3001.sluttdato());
        assertFalse(                        lederRessurs3001.erDagligOppfolging());

        assertEquals(enhet2001,             lederRessurs2001.orgEnhetId());
        assertEquals(leder2001,             lederRessurs2001.lederNavident());
        assertEquals(d1InMonth8MAgo,        lederRessurs2001.startdato());
        assertEquals(lastDayInMonth4MAgo,   lederRessurs2001.sluttdato());
        assertFalse(                        lederRessurs2001.erDagligOppfolging());

        assertEquals(overEnhet10A,          lederRessurs1001After10A.orgEnhetId());
        assertEquals(leder10A,              lederRessurs1001After10A.lederNavident());
        assertEquals(d16InMonth6MAgo,       lederRessurs1001After10A.startdato());
        assertEquals(lastDayInMonth4MAgo,   lederRessurs1001After10A.sluttdato());
        assertTrue(                         lederRessurs1001After10A.erDagligOppfolging());

        assertEquals(overEnhet10B,          lederRessurs1001After10B1.orgEnhetId());
        assertEquals(leder10B1,             lederRessurs1001After10B1.lederNavident());
        assertEquals(d1InMonth3MAgo,        lederRessurs1001After10B1.startdato());
        assertEquals(d15InMonth3MAgo,       lederRessurs1001After10B1.sluttdato());
        assertTrue(                         lederRessurs1001After10B1.erDagligOppfolging());

        assertEquals(overEnhet10B,          lederRessurs1001After10B2.orgEnhetId());
        assertEquals(leder10B2,             lederRessurs1001After10B2.lederNavident());
        assertEquals(d16InMonth3MAgo,       lederRessurs1001After10B2.startdato());
        assertNull(                         lederRessurs1001After10B2.sluttdato());
        assertTrue(                         lederRessurs1001After10B2.erDagligOppfolging());

        // When
        result = orgEnhetLederService.getLedereForRessurs(navident, true);

        // Then
        assertEquals(5, result.size());

        lederRessurs4001 = result.getFirst();
        lederRessurs1001Before = result.get(1);

        lederRessurs1001After10A = result.get(2);
        lederRessurs1001After10B1 = result.get(3);
        lederRessurs1001After10B2 = result.get(4);

        assertEquals(enhet4001,             lederRessurs4001.orgEnhetId());
        assertEquals(leder4001,             lederRessurs4001.lederNavident());
        assertEquals(d1InMonth3YAgo,        lederRessurs4001.startdato());
        assertEquals(lastDayInMonth2YAgo,   lederRessurs4001.sluttdato());
        assertTrue(                         lederRessurs4001.erDagligOppfolging());

        assertEquals(enhet1001,             lederRessurs1001Before.orgEnhetId());
        assertEquals(leder1001Before,       lederRessurs1001Before.lederNavident());
        assertEquals(d1InMonth1YAgo,        lederRessurs1001Before.startdato());
        assertEquals(d15InMonth6MAgo,       lederRessurs1001Before.sluttdato());
        assertTrue(                         lederRessurs1001Before.erDagligOppfolging());

        assertEquals(overEnhet10A,          lederRessurs1001After10A.orgEnhetId());
        assertEquals(leder10A,              lederRessurs1001After10A.lederNavident());
        assertEquals(d16InMonth6MAgo,       lederRessurs1001After10A.startdato());
        assertEquals(lastDayInMonth4MAgo,   lederRessurs1001After10A.sluttdato());
        assertTrue(                         lederRessurs1001After10A.erDagligOppfolging());

        assertEquals(overEnhet10B,          lederRessurs1001After10B1.orgEnhetId());
        assertEquals(leder10B1,             lederRessurs1001After10B1.lederNavident());
        assertEquals(d1InMonth3MAgo,        lederRessurs1001After10B1.startdato());
        assertEquals(d15InMonth3MAgo,       lederRessurs1001After10B1.sluttdato());
        assertTrue(                         lederRessurs1001After10B1.erDagligOppfolging());

        assertEquals(overEnhet10B,          lederRessurs1001After10B2.orgEnhetId());
        assertEquals(leder10B2,             lederRessurs1001After10B2.lederNavident());
        assertEquals(d16InMonth3MAgo,       lederRessurs1001After10B2.startdato());
        assertNull(                         lederRessurs1001After10B2.sluttdato());
        assertTrue(                         lederRessurs1001After10B2.erDagligOppfolging());
    }

    @Test
    @DisplayName("get active ledere for Ressurs - Filters orgtilknytning to only DOL")
    void getAktiveLedereForRessurs_filtersWhenKunDOLIsTrue() {
        // Given
        String navident = "A000001";
        String leder1001 = "B001001";

        var otStartDate = LocalDate.now().minusDays(10);
        var otEndDate = LocalDate.now().plusDays(10);

        var orgtilknytning = List.of(
            OrgtilknytningState.builder().orgEnhetId("1001").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(true).build(),
            OrgtilknytningState.builder().orgEnhetId("1002").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(false).build()
        );
        doReturn(orgtilknytning).when(orgtilknytningService).getByNavIdent(navident);

        var lederStates1001 = List.of(OrgEnhetLederState.builder().navident(leder1001).orgEnhetId("1001").startdato(LocalDate.now().minusDays(20)).sluttdato(null).build());

        doReturn(lederStates1001).when(orgEnhetLederStateRepository).findAllByOrgEnhetId("1001");

        // When
        var result = orgEnhetLederService.getAktiveLedereForRessurs(navident, true);

        // Then
        assertEquals(1, result.size());
        assertEquals("1001", result.getFirst().orgEnhetId());
        assertEquals(leder1001, result.getFirst().lederNavident());
        assertEquals(otStartDate, result.getFirst().startdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertTrue(result.getFirst().erDagligOppfolging());
    }

    @Test
    @DisplayName("get active ledere for Ressurs - Returns empty list when no orgtilknytning exists for navident - kunDOL = false")
    void getAktiveLedereForRessurs_noOrgtilknytningForNavident() {
        // Given
        String navident = "A000001";
        doReturn(Collections.emptyList()).when(orgtilknytningService).getByNavIdent(navident);

        // When
        var result = orgEnhetLederService.getAktiveLedereForRessurs(navident, false);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get active ledere for Ressurs - Returns empty list when no orgtilknytning exists for navident - kunDOL = true")
    void getAktiveLedereForRessurs_noOrgtilknytningForNavident_DOL_true() {
        // Given
        String navident = "A000001";
        doReturn(Collections.emptyList()).when(orgtilknytningService).getByNavIdent(navident);

        // When
        var result = orgEnhetLederService.getAktiveLedereForRessurs(navident, true);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get active ledere for Ressurs - Filters orgtilknytning to not only DOL")
    void getAktiveLedereForRessurs_filtersWhenKunDOLIsFalse() {
        // Given
        String navident = "A000001";
        String leder1001 = "B001001";
        String leder1002 = "B001002";

        var otStartDate = LocalDate.now().minusDays(10);
        var otEndDate = LocalDate.now().plusDays(10);

        var orgtilknytning = List.of(
            OrgtilknytningState.builder().orgEnhetId("1001").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(true).build(),
            OrgtilknytningState.builder().orgEnhetId("1002").gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(false).build()
        );
        doReturn(orgtilknytning).when(orgtilknytningService).getByNavIdent(navident);

        var lederStates1001 = List.of(OrgEnhetLederState.builder().navident(leder1001).orgEnhetId("1001").startdato(LocalDate.now().minusDays(20)).sluttdato(null).build());
        var lederStates1002 = List.of(OrgEnhetLederState.builder().navident(leder1002).orgEnhetId("1002").startdato(LocalDate.now().minusDays(20)).sluttdato(null).build());

        doReturn(lederStates1001).when(orgEnhetLederStateRepository).findAllByOrgEnhetId("1001");
        doReturn(lederStates1002).when(orgEnhetLederStateRepository).findAllByOrgEnhetId("1002");

        // When
        var result = orgEnhetLederService.getAktiveLedereForRessurs(navident, false);

        // Then
        assertEquals(2, result.size());
        assertEquals("1001", result.getFirst().orgEnhetId());
        assertEquals(leder1001, result.getFirst().lederNavident());
        assertEquals(otStartDate, result.getFirst().startdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertTrue(result.getFirst().erDagligOppfolging());

        assertEquals("1002", result.getLast().orgEnhetId());
        assertEquals(leder1002, result.getLast().lederNavident());
        assertFalse(result.getLast().erDagligOppfolging());
    }

    @Test
    @DisplayName("get active ledere for Ressurs - Includes leaders from over-enheter when navident matches")
    void getAktiveLedereForRessurs_includesLeadersFromOverEnheterWhenNavidentMatches() {
        // Given
        String navident = "A000001";
        String leder10 = "B000010";
        String enhet1001    = "1001";
        String overEnhet100 = "100";
        String overEnhet10  = "10";

        var otStartDate = LocalDate.now().minusDays(10);
        var otEndDate = LocalDate.now().plusDays(10);

        var orgtilknytning = List.of(
            OrgtilknytningState.builder().orgEnhetId(enhet1001).gyldigFom(otStartDate).gyldigTom(otEndDate).erDagligOppfolging(true).build()
        );
        var lederState1001  = OrgEnhetLederState.builder().navident(navident).orgEnhetId(enhet1001).startdato(otStartDate).sluttdato(otEndDate).build();
        var lederState100   = OrgEnhetLederState.builder().navident(navident).orgEnhetId(overEnhet100).startdato(otStartDate).sluttdato(otEndDate).build();
        var lederState10    = OrgEnhetLederState.builder().navident(leder10).orgEnhetId(overEnhet10).startdato(otStartDate).sluttdato(otEndDate).build();

        doReturn(orgtilknytning).when(orgtilknytningService).getByNavIdent(navident);
        doReturn(List.of(lederState1001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet1001);
        doReturn(List.of(lederState100)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet100);
        doReturn(List.of(lederState10)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet10);

        var organiseringStateForId1001 = List.of(
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100).navnOver("Enhet-"+overEnhet100).gyldigFom(otStartDate.minusYears(1)).build()
        );
        var organiseringStateForId100 = List.of(
            OrganiseringState.builder().id(overEnhet100).navn("Enhet-"+overEnhet100).idOver(overEnhet10).navnOver("Enhet-"+overEnhet10).gyldigFom(otStartDate.minusYears(1)).build(),
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100).navnOver("Enhet-"+overEnhet100).gyldigFom(otStartDate.minusYears(1)).build()
        );
        doReturn(organiseringStateForId1001).when(organiseringStateRepository).findById(enhet1001);
        doReturn(organiseringStateForId100).when(organiseringStateRepository).findById(overEnhet100);

        // When
        var result = orgEnhetLederService.getAktiveLedereForRessurs(navident, false);

        // Then
        assertEquals(1, result.size());
        assertEquals("10", result.getFirst().orgEnhetId());
        assertEquals(leder10, result.getFirst().lederNavident());
        assertEquals(otStartDate, result.getFirst().startdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertEquals(otEndDate, result.getFirst().sluttdato());
        assertTrue(result.getFirst().erDagligOppfolging());
    }

    @Test
    @DisplayName("get active ledere for Ressurs - Handles different complex cases with different periods")
    void getAktiveLedereForRessurs_handlesOverlappingPeriodsForOverEnheter() {
        // Given
        String navident = "A000001";

        String leder1001Before = "B001001";
        String leder2001 = "B002001";
        String leder3001 = "B003001";
        String leder4001 = "B004001";
        String leder10A = "B000010A";
        String leder10B1 = "B000010B1";
        String leder10B2 = "B000010B2";

        String enhet2001    = "2001";
        String enhet3001    = "3001";
        String enhet4001    = "4001";

        String enhet1001    = "1001";
        String overEnhet100A = "100A";
        String overEnhet100B = "100B";
        String overEnhet10A  = "10A";
        String overEnhet10B  = "10B";

        var d1InMonth3YAgo = LocalDate.now().minusYears(3).withDayOfMonth(1);
        var lastDayInMonth2YAgo = LocalDate.now().minusYears(2).withDayOfMonth(1).minusDays(1);
        var d1InMonth1YAgo = LocalDate.now().minusYears(1).withDayOfMonth(1);
        var d1InMonth8MAgo = LocalDate.now().minusMonths(8).withDayOfMonth(1);
        var d15InMonth6MAgo = LocalDate.now().minusMonths(6).withDayOfMonth(15);
        var d16InMonth6MAgo =  LocalDate.now().minusMonths(6).withDayOfMonth(16);
        var d15InMonth3MAgo = LocalDate.now().minusMonths(3).withDayOfMonth(15);
        var d16InMonth3MAgo =  LocalDate.now().minusMonths(3).withDayOfMonth(16);
        var lastDayInMonth4MAgo = LocalDate.now().minusMonths(3).withDayOfMonth(1).minusDays(1);
        var d1InMonth3MAgo = LocalDate.now().minusMonths(3).withDayOfMonth(1);

        // Orgtilknytninger

        //           3YAgo                2YAgo              1YAgo               8MAgo               6M+15d Ago                   3MAgo    3M+15d Ago
        //             |                    |                  |                   |                     |                          |          |
        //-------------><-------------------><-----------------><------------------><--------------------><-------------------------><---------><--------------
        //                                                      <------------ 1001 DOL ------------------------------------------------------------------------
        //                                                                          <------ 2001 ----------------------------------->
        //                                                      <------ 3001 ------------------------------------------------------->
        //              <---- 4001 DOL ----->
        var orgtilknytninger = List.of(
            OrgtilknytningState.builder().orgEnhetId(enhet1001).gyldigFom(d1InMonth1YAgo).gyldigTom(null).erDagligOppfolging(true).build(),
            OrgtilknytningState.builder().orgEnhetId(enhet2001).gyldigFom(d1InMonth8MAgo).gyldigTom(lastDayInMonth4MAgo).erDagligOppfolging(false).build(),
            OrgtilknytningState.builder().orgEnhetId(enhet3001).gyldigFom(d1InMonth1YAgo).gyldigTom(lastDayInMonth4MAgo).erDagligOppfolging(false).build(),
            OrgtilknytningState.builder().orgEnhetId(enhet4001).gyldigFom(d1InMonth3YAgo).gyldigTom(lastDayInMonth2YAgo).erDagligOppfolging(true).build()
        );
        doReturn(orgtilknytninger).when(orgtilknytningService).getByNavIdent(navident);

        // Ledere
        //             3YAgo                2YAgo              1YAgo               8MAgo             6M+15d Ago                   3MAgo    3M+15d Ago
        //               |                    |                  |                   |                   |                          |          |
        // Enhet  -------><-------------------><-----------------><------------------><------------------><-------------------------><---------><--------------
        //  2001          <---- leder2001 ---------------------------------------------------------------------------------------------------------------------
        //  3001          <---- leder3001 ---------------------------------------------------------------------------------------------------------------------
        //  4001          <---- leder4001 ---------------------------------------------------------------------------------------------------------------------
        //  1001          <---- leder1001Before ---------------------------------------------------------><-------- navident ----------------------------------
        //  100A                                                                                          <-------- navident ----------------------------------
        //  100B                                                                                          <-------- navident ----------------------------------
        //  10A                                                   <------------------------------------------------ leder10A ----------------------------------
        //  10B                                                   <------------------------------------------------ leder10B1 -----------------><--- leder10B2 -

        var lederState2001  = OrgEnhetLederState.builder().navident(leder2001).orgEnhetId(enhet2001).startdato(d1InMonth3YAgo).sluttdato(null).build();
        var lederState3001  = OrgEnhetLederState.builder().navident(leder3001).orgEnhetId(enhet3001).startdato(d1InMonth3YAgo).sluttdato(null).build();
        var lederState4001  = OrgEnhetLederState.builder().navident(leder4001).orgEnhetId(enhet4001).startdato(d1InMonth3YAgo).sluttdato(null).build();

        var lederState1001Before  = OrgEnhetLederState.builder().navident(leder1001Before).orgEnhetId(enhet1001).startdato(d1InMonth3YAgo).sluttdato(d15InMonth6MAgo).build();
        var lederState1001Now  = OrgEnhetLederState.builder().navident(navident).orgEnhetId(enhet1001).startdato(d16InMonth6MAgo).sluttdato(null).build();

        var lederState100A   = OrgEnhetLederState.builder().navident(navident).orgEnhetId(overEnhet100A).startdato(d16InMonth6MAgo).sluttdato(null).build();
        var lederState100B    = OrgEnhetLederState.builder().navident(navident).orgEnhetId(overEnhet100B).startdato(d16InMonth6MAgo).sluttdato(null).build();

        var lederState10A   = OrgEnhetLederState.builder().navident(leder10A).orgEnhetId(overEnhet10A).startdato(d1InMonth1YAgo).sluttdato(null).build();

        var lederState10B1    = OrgEnhetLederState.builder().navident(leder10B1).orgEnhetId(overEnhet10B).startdato(d1InMonth1YAgo).sluttdato(d15InMonth3MAgo).build();
        var lederState10B2    = OrgEnhetLederState.builder().navident(leder10B2).orgEnhetId(overEnhet10B).startdato(d16InMonth3MAgo).sluttdato(null).build();

        doReturn(List.of(lederState2001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet2001);
        doReturn(List.of(lederState3001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet3001);
        doReturn(List.of(lederState4001)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet4001);

        doReturn(List.of(lederState1001Before, lederState1001Now)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(enhet1001);

        doReturn(List.of(lederState100A)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet100A);
        doReturn(List.of(lederState100B)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet100B);
        doReturn(List.of(lederState10A)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet10A);
        doReturn(List.of(lederState10B1, lederState10B2)).when(orgEnhetLederStateRepository).findAllByOrgEnhetId(overEnhet10B);

        // Organisering/hierarki
        //             3YAgo                2YAgo              1YAgo               8MAgo             6M+15d Ago                   3MAgo    3M+15d Ago
        //               |                    |                  |                   |                   |                          |          |
        // Enhet  -------><-------------------><-----------------><------------------><------------------><-------------------------><---------><--------------
        //  1001          <--------------------------------------------- 100A ------------------------------------------------------><---- 100B ---------------
        //  100A          <---------------------------------------------- 10A ---------------------------------------------------------------------------------
        //  100B          <---------------------------------------------- 10B ---------------------------------------------------------------------------------
        var organiseringStateForId1001 = List.of(
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100A).navnOver("Enhet-"+overEnhet100A).gyldigFom(d1InMonth3YAgo).gyldigTom(lastDayInMonth4MAgo).build(),
            OrganiseringState.builder().id(enhet1001).navn("Enhet-"+enhet1001).idOver(overEnhet100B).navnOver("Enhet-"+overEnhet100B).gyldigFom(d1InMonth3MAgo).build()
        );
        var organiseringStateForId100A = List.of(
            OrganiseringState.builder().id(overEnhet100A).navn("Enhet-"+overEnhet100A).idOver(overEnhet10A).navnOver("Enhet-"+overEnhet10A).gyldigFom(d1InMonth3YAgo).gyldigTom(lastDayInMonth4MAgo).build()
        );
        var organiseringStateForId100B = List.of(
            OrganiseringState.builder().id(overEnhet100B).navn("Enhet-"+overEnhet100B).idOver(overEnhet10B).navnOver("Enhet-"+overEnhet10B).gyldigFom(d1InMonth3YAgo).build()
        );

        doReturn(organiseringStateForId1001).when(organiseringStateRepository).findById(enhet1001);
        doReturn(organiseringStateForId100A).when(organiseringStateRepository).findById(overEnhet100A);
        doReturn(organiseringStateForId100B).when(organiseringStateRepository).findById(overEnhet100B);

        // When
        var result = orgEnhetLederService.getAktiveLedereForRessurs(navident, false);

        // Then
        assertEquals(1, result.size());

        var lederRessurs1001After10B2 = result.getFirst();

        assertEquals(overEnhet10B,          lederRessurs1001After10B2.orgEnhetId());
        assertEquals(leder10B2,             lederRessurs1001After10B2.lederNavident());
        assertEquals(d16InMonth3MAgo,       lederRessurs1001After10B2.startdato());
        assertNull(                         lederRessurs1001After10B2.sluttdato());
        assertTrue(                         lederRessurs1001After10B2.erDagligOppfolging());

        // When
        result = orgEnhetLederService.getAktiveLedereForRessurs(navident, true);

        // Then
        assertEquals(1, result.size());

        lederRessurs1001After10B2 = result.getFirst();

        assertEquals(overEnhet10B,          lederRessurs1001After10B2.orgEnhetId());
        assertEquals(leder10B2,             lederRessurs1001After10B2.lederNavident());
        assertEquals(d16InMonth3MAgo,       lederRessurs1001After10B2.startdato());
        assertNull(                         lederRessurs1001After10B2.sluttdato());
        assertTrue(                         lederRessurs1001After10B2.erDagligOppfolging());
    }


    @Test
    @DisplayName("Test at løsningen klarer å avslutte en OrgEnhetLederState når orgenheten faller ut av hierarkiet")
    void test_007() {
        // Given
        var orgEnhetIdEn = "ab123c";
        var orgEnhetIdTo = "cd456e";
        var orgEnhetIdTre = "fg789h";
        var dvhLeder = List.of(
            DvhLeder.builder().navIdent("A000001").orgEnhetNr("1000000").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1A").build(),
            DvhLeder.builder().navIdent("A000002").orgEnhetNr("1000001").orgNiv("ORGNIV4").orgenhetNavn("Utvikling 1B").build()
        );

        var nomLeder = List.of(
            OrgEnhetLederState.builder().fid("FID-001").navident("A000001").orgEnhetId(orgEnhetIdEn).startdato(LocalDate.now().minusMonths(1)).build(),
            OrgEnhetLederState.builder().fid("FID-002").navident("A000001").orgEnhetId(orgEnhetIdTo).startdato(LocalDate.now()).build()
        );

        var nomLederMap = Map.of(
            nomLeder.getFirst().navident(), Map.of(nomLeder.getFirst().orgEnhetId(), nomLeder.getFirst(), nomLeder.getLast().orgEnhetId(), nomLeder.getLast())
        );

        // And
        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetIdTo).build())))
            .when(orgenhetService)
            .getEnhetByAgressoIdNivaAndName(
                dvhLeder.getFirst().getOrgEnhetNr(),
                dvhLeder.getFirst().getOrgNiv(),
                dvhLeder.getFirst().getOrgenhetNavn()
            );

        doReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder().id(orgEnhetIdTre).build())))
            .when(orgenhetService)
            .getEnhetByAgressoIdNivaAndName(
                dvhLeder.getLast().getOrgEnhetNr(),
                dvhLeder.getLast().getOrgNiv(),
                dvhLeder.getLast().getOrgenhetNavn()
            );

        doReturn(List.of(nomLeder.getFirst()))
            .when(orgEnhetLederStateRepository)
            .findAllByOrgEnhetId(orgEnhetIdTo);

        doReturn("FID-003")
            .when(orgEnhetLederCommandService)
            .opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));

        doReturn(nomLederMap)
            .when(orgEnhetLederStateRepository)
            .getOrgEnhetLederByNavidentMap();

        // When
        orgEnhetLederService.persisterLedereForOrgenheter(dvhLeder);

        // Then
        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederSluttdato(anyString(), any());
    }
}
