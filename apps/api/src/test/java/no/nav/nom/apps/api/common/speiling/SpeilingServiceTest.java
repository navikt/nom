package no.nav.nom.apps.api.common.speiling;

import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterTilknytningtypeCommand;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpeilingServiceTest {

    @Mock
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    RessursStateRepository ressursStateRepository;

    @Mock
    NomMastered nomMastered;

    @Mock
    LeaderElection leaderElection;

    @Mock
    RessursCommandService ressursCommandService;

    SpeilingService speilingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        speilingService = new SpeilingService(
            orgtilknytningStateRepository,
            ressursStateRepository,
            nomMastered,
            leaderElection,
            ressursCommandService
        );
    }

    private OrgtilknytningState activeOt(String orgEnhetId, boolean dagligOppfolging) {
        return OrgtilknytningState.builder()
            .fid("ot-fid-A000001")
            .navident("A000001")
            .orgEnhetId(orgEnhetId)
            .erDagligOppfolging(dagligOppfolging)
            .gyldigFom(LocalDate.now().minusDays(30))
            .gyldigTom(LocalDate.now().plusDays(30))
            .build();
    }

    private RessursState ressursState() {
        return RessursState.builder()
            .fid("fid1")
            .navident("A000001")
            .personident("12345678901")
            .fysiskeBrukersteder(Collections.emptyList())
            .build();
    }

    @Nested
    @DisplayName("speilOrgtilknytningSektorTilRessursScheduler")
    class SchedulerTests {

        @Test
        void scheduler_doesNotRun_whenNotLeader() {
            when(leaderElection.isLeader()).thenReturn(false);

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verifyNoInteractions(orgtilknytningStateRepository);
            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void scheduler_runs_whenLeader() {
            when(leaderElection.isLeader()).thenReturn(true);
            when(orgtilknytningStateRepository.getAll()).thenReturn(Collections.emptyList());

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verify(orgtilknytningStateRepository).getAll();
        }
    }

    @Nested
    @DisplayName("speilOrgtilknytningDataTilRessursState (via scheduler)")
    class SpeilDataTests {

        @BeforeEach
        void setUpLeader() {
            when(leaderElection.isLeader()).thenReturn(true);
        }

        @Test
        void updatesSektorOnRessurs_whenDagligOppfolgingHasDifferentSektor() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_KOMMUNAL).build();
            var rs = ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            var captor = ArgumentCaptor.forClass(OppdaterSektorCommand.class);
            verify(ressursCommandService).oppdaterSektorEvent(captor.capture(), any(InputMetadata.class));
            assertEquals("fid1", captor.getValue().fid());
            assertEquals(Sektor.NAV_KOMMUNAL, captor.getValue().sektor());
        }

        @Test
        void doesNotUpdateSektor_whenSektorAlreadyMatches() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_STATLIG).build();
            var rs = ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verify(ressursCommandService, never()).oppdaterSektorEvent(any(), any());
        }

        @Test
        void updatesSektorOnRessurs_whenRessursSektorIsNull() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_KOMMUNAL).build();
            var rs = ressursState();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verify(ressursCommandService).oppdaterSektorEvent(any(OppdaterSektorCommand.class), any(InputMetadata.class));
        }

        @Test
        void doesNotUpdateSektor_whenOtSektorIsNull() {
            var ot = activeOt( "AB123C", true);
            var rs = ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verify(ressursCommandService, never()).oppdaterSektorEvent(any(), any());
        }

        @Test
        void updatesTilknytningtype_whenDifferent() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .orgtilknytningType(Tilknytningtype.FAST).build();
            var rs = ressursState().toBuilder()
                .tilknytningtype(Tilknytningtype.MIDLERTIDIG).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            var captor = ArgumentCaptor.forClass(OppdaterTilknytningtypeCommand.class);
            verify(ressursCommandService).oppdaterTilknytningtypeEvent(captor.capture(), any(InputMetadata.class));
            assertEquals(Tilknytningtype.FAST, captor.getValue().tilknytningtype());
        }

        @Test
        void doesNotUpdateTilknytningtype_whenAlreadyMatches() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .orgtilknytningType(Tilknytningtype.FAST).build();
            var rs = ressursState().toBuilder()
                .tilknytningtype(Tilknytningtype.FAST).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verify(ressursCommandService, never()).oppdaterTilknytningtypeEvent(any(), any());
        }

        @Test
        void updatesFysiskeBrukersteder_whenPrimaryDiffers() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1234").build();
            var rs = ressursState().toBuilder()
                .primaryFysiskBrukersted("9999").build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            var captor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
            verify(ressursCommandService).oppdaterFysiskeBrukerstederCommand(captor.capture(), any(InputMetadata.class));
            assertEquals("1234", captor.getValue().primaryFysiskBrukersted());
        }

        @Test
        void updatesFysiskeBrukersteder_whenAdditionalBrukerstederDiffer() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1234")
                .additionalFysiskeBrukerstederNummer(List.of("5678"))
                .build();
            var rs = ressursState().toBuilder()
                .primaryFysiskBrukersted("1234")
                .fysiskeBrukersteder(List.of("1234"))
                .build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            var captor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
            verify(ressursCommandService).oppdaterFysiskeBrukerstederCommand(captor.capture(), any(InputMetadata.class));
            // alleFysiskeBrukersteder should contain both primary and additional from all OTs
            assertTrue(captor.getValue().fysiskeBrukersteder().containsAll(Set.of("1234", "5678")));
        }

        @Test
        void doesNotUpdateFysiskeBrukersteder_whenAllMatch() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1234")
                .build();
            var rs = ressursState().toBuilder()
                .primaryFysiskBrukersted("1234")
                .fysiskeBrukersteder(List.of("1234"))
                .build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verify(ressursCommandService, never()).oppdaterFysiskeBrukerstederCommand(any(), any());
        }

        @Test
        void skipsNonMasteredNavident() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_KOMMUNAL).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(false);

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verifyNoInteractions(ressursCommandService);
            verify(ressursStateRepository, never()).findByNavident(any());
        }

        @Test
        void skipsExpiredPeriod() {
            var ot = OrgtilknytningState.builder()
                .fid("ot-fid")
                .navident("A000001")
                .orgEnhetId("AB123C")
                .erDagligOppfolging(true)
                .gyldigFom(LocalDate.now().minusDays(60))
                .gyldigTom(LocalDate.now().minusDays(30))
                .sektor(Sektor.NAV_KOMMUNAL)
                .build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void skipsRessursNotFound() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_KOMMUNAL).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.empty());

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void onlyDagligOppfolgingSetsPrimaryBrukersted() {
            var otDol = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1111")
                .build();
            var otNonDol = activeOt( "CD456E", false).toBuilder()
                .primaryFysiskBrukerstedNummer("2222")
                .build();
            var rs = ressursState();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(otDol, otNonDol));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilOrgtilknytningSektorTilRessursScheduler();

            var captor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
            verify(ressursCommandService).oppdaterFysiskeBrukerstederCommand(captor.capture(), any(InputMetadata.class));
            assertEquals("1111", captor.getValue().primaryFysiskBrukersted());
            assertTrue(captor.getValue().fysiskeBrukersteder().containsAll(Set.of("1111", "2222")));
        }
    }

    @Nested
    @DisplayName("speilSektorOrgtilknytning")
    class SpeilSektorTests {

        @Test
        void updatesSektor_whenRessursHasDifferentSektor() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_KOMMUNAL).build();
            var rs = ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilSektorOrgtilknytning();

            verify(ressursCommandService).oppdaterSektorEvent(any(OppdaterSektorCommand.class), any(InputMetadata.class));
        }

        @Test
        void skipsIfSektorMatches() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_STATLIG).build();
            var rs = ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilSektorOrgtilknytning();

            verify(ressursCommandService, never()).oppdaterSektorEvent(any(), any());
        }

        @Test
        void filtersOutNonDagligOppfolging() {
            var ot = activeOt( "AB123C", false).toBuilder()
                .sektor(Sektor.NAV_KOMMUNAL).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));

            speilingService.speilSektorOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void filtersOutNullSektor() {
            var ot = activeOt( "AB123C", true);

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));

            speilingService.speilSektorOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void filtersOutNonMastered() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.NAV_KOMMUNAL).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(false);

            speilingService.speilSektorOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void filtersOutInactivePeriods() {
            var ot = OrgtilknytningState.builder()
                .fid("ot-fid")
                .navident("A000001")
                .orgEnhetId("AB123C")
                .erDagligOppfolging(true)
                .gyldigFom(LocalDate.now().minusDays(60))
                .gyldigTom(LocalDate.now().minusDays(30))
                .sektor(Sektor.NAV_KOMMUNAL)
                .build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));

            speilingService.speilSektorOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void updatesSektor_whenRessursSektorIsNull() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .sektor(Sektor.EKSTERN).build();
            var rs = ressursState();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilSektorOrgtilknytning();

            var captor = ArgumentCaptor.forClass(OppdaterSektorCommand.class);
            verify(ressursCommandService).oppdaterSektorEvent(captor.capture(), any(InputMetadata.class));
            assertEquals(Sektor.EKSTERN, captor.getValue().sektor());
        }
    }

    @Nested
    @DisplayName("speilTilknytningtypeOrgtilknytning")
    class SpeilTilknytningtypeTests {

        @Test
        void updatesType_whenDifferent() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .orgtilknytningType(Tilknytningtype.FAST).build();
            var rs = ressursState().toBuilder()
                .tilknytningtype(Tilknytningtype.VIKAR).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilTilknytningtypeOrgtilknytning();

            var captor = ArgumentCaptor.forClass(OppdaterTilknytningtypeCommand.class);
            verify(ressursCommandService).oppdaterTilknytningtypeEvent(captor.capture(), any(InputMetadata.class));
            assertEquals(Tilknytningtype.FAST, captor.getValue().tilknytningtype());
        }

        @Test
        void skipsIfTypeMatches() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .orgtilknytningType(Tilknytningtype.FAST).build();
            var rs = ressursState().toBuilder()
                .tilknytningtype(Tilknytningtype.FAST).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilTilknytningtypeOrgtilknytning();

            verify(ressursCommandService, never()).oppdaterTilknytningtypeEvent(any(), any());
        }

        @Test
        void filtersOutNullOrgtilknytningType() {
            var ot = activeOt( "AB123C", true);

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));

            speilingService.speilTilknytningtypeOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void filtersOutNonDagligOppfolging() {
            var ot = activeOt( "AB123C", false).toBuilder()
                .orgtilknytningType(Tilknytningtype.FAST).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));

            speilingService.speilTilknytningtypeOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void filtersOutNonMastered() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .orgtilknytningType(Tilknytningtype.FAST).build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(false);

            speilingService.speilTilknytningtypeOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void updatesType_whenRessursTilknytningtypeIsNull() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .orgtilknytningType(Tilknytningtype.MIDLERTIDIG).build();
            var rs = ressursState();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilTilknytningtypeOrgtilknytning();

            verify(ressursCommandService).oppdaterTilknytningtypeEvent(any(OppdaterTilknytningtypeCommand.class), any(InputMetadata.class));
        }
    }

    @Nested
    @DisplayName("speilBrukerstederOrgtilknytning")
    class SpeilBrukerstederTests {

        @Test
        void updatesBrukersteder_whenPrimaryChanged() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1234").build();
            var rs = ressursState().toBuilder()
                .primaryFysiskBrukersted("9999").build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilBrukerstederOrgtilknytning();

            var captor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
            verify(ressursCommandService).oppdaterFysiskeBrukerstederCommand(captor.capture(), any(InputMetadata.class));
            assertEquals("1234", captor.getValue().primaryFysiskBrukersted());
        }

        @Test
        void updatesBrukersteder_whenAdditionalChanged() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1234")
                .additionalFysiskeBrukerstederNummer(List.of("5678", "9012"))
                .build();
            var rs = ressursState().toBuilder()
                .primaryFysiskBrukersted("1234")
                .fysiskeBrukersteder(List.of("1234"))
                .build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilBrukerstederOrgtilknytning();

            var captor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
            verify(ressursCommandService).oppdaterFysiskeBrukerstederCommand(captor.capture(), any(InputMetadata.class));
            // alleFysiskeBrukersteder should contain both primary and all additional from all OTs
            assertTrue(captor.getValue().fysiskeBrukersteder().containsAll(Set.of("1234", "5678", "9012")));
        }

        @Test
        void doesNotUpdate_whenAllBrukerstederMatch() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1234")
                .build();
            var rs = ressursState().toBuilder()
                .primaryFysiskBrukersted("1234")
                .fysiskeBrukersteder(List.of("1234"))
                .build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilBrukerstederOrgtilknytning();

            verify(ressursCommandService, never()).oppdaterFysiskeBrukerstederCommand(any(), any());
        }

        @Test
        void collectsBrukerstederFromMultipleOrgtilknytninger() {
            var ot1 = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1111")
                .additionalFysiskeBrukerstederNummer(List.of("2222"))
                .build();
            var ot2 = activeOt( "CD456E", false).toBuilder()
                .primaryFysiskBrukerstedNummer("3333")
                .build();
            var rs = ressursState();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot1, ot2));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilBrukerstederOrgtilknytning();

            var captor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
            verify(ressursCommandService).oppdaterFysiskeBrukerstederCommand(captor.capture(), any(InputMetadata.class));
            assertEquals("1111", captor.getValue().primaryFysiskBrukersted());
            assertTrue(captor.getValue().fysiskeBrukersteder().containsAll(Set.of("1111", "2222", "3333")));
        }

        @Test
        void onlyDagligOppfolging_setsPrimaryBrukersted() {
            var otNonDol = activeOt( "AB123C", false).toBuilder()
                .primaryFysiskBrukerstedNummer("1234").build();
            var rs = ressursState();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(otNonDol));
            when(nomMastered.isMastered("A000001")).thenReturn(true);
            when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(rs));

            speilingService.speilBrukerstederOrgtilknytning();

            var captor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
            verify(ressursCommandService).oppdaterFysiskeBrukerstederCommand(captor.capture(), any(InputMetadata.class));
            assertNull(captor.getValue().primaryFysiskBrukersted());
            assertTrue(captor.getValue().fysiskeBrukersteder().contains("1234"));
        }

        @Test
        void filtersOutInactivePeriods() {
            var ot = OrgtilknytningState.builder()
                .fid("ot-fid")
                .navident("A000001")
                .orgEnhetId("AB123C")
                .erDagligOppfolging(true)
                .gyldigFom(LocalDate.now().minusDays(60))
                .gyldigTom(LocalDate.now().minusDays(30))
                .primaryFysiskBrukerstedNummer("1234")
                .build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));

            speilingService.speilBrukerstederOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }

        @Test
        void filtersOutNonMastered() {
            var ot = activeOt( "AB123C", true).toBuilder()
                .primaryFysiskBrukerstedNummer("1234").build();

            when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(ot));
            when(nomMastered.isMastered("A000001")).thenReturn(false);

            speilingService.speilBrukerstederOrgtilknytning();

            verifyNoInteractions(ressursCommandService);
        }
    }
}
