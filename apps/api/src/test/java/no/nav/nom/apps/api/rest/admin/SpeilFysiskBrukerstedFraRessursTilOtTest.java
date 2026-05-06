package no.nav.nom.apps.api.rest.admin;

import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.RessursService;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpeilFysiskBrukerstedFraRessursTilOtTest {

    @Mock
    RessursService ressursService;

    @Mock
    OrgtilknytningService orgtilknytningService;

    @Mock
    OrgtilknytningCommandService orgtilknytningCommandService;

    MockedStatic<SecurityUtil> securityUtilMock;

    SpeilFysiskBrukerstedFraRessursTilOt controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SpeilFysiskBrukerstedFraRessursTilOt(
            ressursService, orgtilknytningService, orgtilknytningCommandService
        );

        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getAuthenticatedNavIdent).thenReturn("A000001");
        securityUtilMock.when(SecurityUtil::getAuthenticatedAzpName).thenReturn("test-app");
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    private RessursState ressursWithPrimary(String primaryBrukersted) {
        return RessursState.builder()
            .fid("fid-A000001")
            .navident("A000001")
            .personident("12345678901")
            .sektor(Sektor.NAV_STATLIG)
            .primaryFysiskBrukersted(primaryBrukersted)
            .fysiskeBrukersteder(Collections.emptyList())
            .build();
    }

    // OTs don't have primaryFysiskBrukerstedNummer — this migration copies it from RessursState
    private OrgtilknytningState activeOt(String orgEnhetId) {
        return OrgtilknytningState.builder()
            .fid("ot-fid-A000001")
            .navident("A000001")
            .orgEnhetId(orgEnhetId)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusDays(30))
            .gyldigTom(LocalDate.now().plusDays(30))
            .build();
    }

    @Nested
    @DisplayName("dryRun = true")
    class DryRunTests {

        @Test
        void returnsCountOfActiveOTsThatWouldBeUpdated() {
            var rs = ressursWithPrimary("1234");
            var ot = activeOt( "AB123C");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot))).when(orgtilknytningService).getByNavidenter(any(), any());

            String result = controller.speilFysiskBrukerstedFraRessursTilOt(true);

            assertTrue(result.contains("1"));
            assertTrue(result.contains("aktive OT"));
        }

        @Test
        void doesNotCallCommandService() {
            var rs = ressursWithPrimary("1234");
            var ot = activeOt( "AB123C");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot))).when(orgtilknytningService).getByNavidenter(any(), any());

            controller.speilFysiskBrukerstedFraRessursTilOt(true);

            verifyNoInteractions(orgtilknytningCommandService);
        }

        @Test
        void skipsRessursWithNullPrimaryBrukersted() {
            var rs = ressursWithPrimary(null);

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", Collections.emptyList())).when(orgtilknytningService).getByNavidenter(any(), any());

            String result = controller.speilFysiskBrukerstedFraRessursTilOt(true);

            assertTrue(result.contains("0"));
        }

        @Test
        void skipsRessursWithBlankPrimaryBrukersted() {
            var rs = ressursWithPrimary( "   ");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", Collections.emptyList())).when(orgtilknytningService).getByNavidenter(any(), any());

            String result = controller.speilFysiskBrukerstedFraRessursTilOt(true);

            assertTrue(result.contains("0"));
        }

        @Test
        void skipsInactiveOrgtilknytninger() {
            var rs = ressursWithPrimary( "1234");
            var expiredOt = OrgtilknytningState.builder()
                .fid("ot-fid")
                .navident("A000001")
                .orgEnhetId("AB123C")
                .erDagligOppfolging(true)
                .gyldigFom(LocalDate.now().minusDays(60))
                .gyldigTom(LocalDate.now().minusDays(30))
                .build();

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(expiredOt))).when(orgtilknytningService).getByNavidenter(any(), any());

            String result = controller.speilFysiskBrukerstedFraRessursTilOt(true);

            assertTrue(result.contains("0"));
        }
    }

    @Nested
    @DisplayName("dryRun = false")
    class LiveTests {

        @Test
        void sendsEndreFysiskeBrukerstedCommand_forEachActiveOT() {
            var rs = ressursWithPrimary( "1234");
            var ot1 = activeOt( "AB123C");
            var ot2 = activeOt( "CD456E");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot1, ot2))).when(orgtilknytningService).getByNavidenter(any(), any());

            controller.speilFysiskBrukerstedFraRessursTilOt(false);

            verify(orgtilknytningCommandService, times(2)).endreFysiskeBrukersted(any(EndreFysiskeBrukerstederCommand.class));
        }

        @Test
        void returnsEmptyString_whenAllSuccessful() {
            var rs = ressursWithPrimary( "1234");
            var ot = activeOt( "AB123C");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot))).when(orgtilknytningService).getByNavidenter(any(), any());

            String result = controller.speilFysiskBrukerstedFraRessursTilOt(false);

            assertEquals("", result);
        }

        @Test
        void appendsErrorMessage_whenCommandServiceThrows() {
            var rs = ressursWithPrimary( "1234");
            var ot = activeOt( "AB123C");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot))).when(orgtilknytningService).getByNavidenter(any(), any());
            when(orgtilknytningCommandService.endreFysiskeBrukersted(any()))
                .thenThrow(new RuntimeException("Kafka error"));

            String result = controller.speilFysiskBrukerstedFraRessursTilOt(false);

            assertTrue(result.contains("A000001"));
            assertTrue(result.contains("fikk ikke overført"));
        }

        @Test
        void setsAdditionalBrukerstederToEmptyList_whenNull() {
            var rs = ressursWithPrimary( "1234");
            var ot = activeOt( "AB123C");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot))).when(orgtilknytningService).getByNavidenter(any(), any());

            controller.speilFysiskBrukerstedFraRessursTilOt(false);

            var captor = ArgumentCaptor.forClass(EndreFysiskeBrukerstederCommand.class);
            verify(orgtilknytningCommandService).endreFysiskeBrukersted(captor.capture());
            assertNotNull(captor.getValue().additionalFysiskeBrukerstederNummer());
            assertTrue(captor.getValue().additionalFysiskeBrukerstederNummer().isEmpty());
        }

        @Test
        void passesCorrectPrimaryFysiskBrukerstedFromRessurs() {
            var rs = ressursWithPrimary( "5678");
            var ot = activeOt( "AB123C");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot))).when(orgtilknytningService).getByNavidenter(any(), any());

            controller.speilFysiskBrukerstedFraRessursTilOt(false);

            var captor = ArgumentCaptor.forClass(EndreFysiskeBrukerstederCommand.class);
            verify(orgtilknytningCommandService).endreFysiskeBrukersted(captor.capture());
            assertEquals("5678", captor.getValue().primaryFysiskBrukerstedNummer());
        }

        @Test
        void passesCorrectInputMetadata() {
            var rs = ressursWithPrimary( "1234");
            var ot = activeOt( "AB123C");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot))).when(orgtilknytningService).getByNavidenter(any(), any());

            controller.speilFysiskBrukerstedFraRessursTilOt(false);

            var captor = ArgumentCaptor.forClass(EndreFysiskeBrukerstederCommand.class);
            verify(orgtilknytningCommandService).endreFysiskeBrukersted(captor.capture());
            var metadata = captor.getValue().inputMetadata();
            assertEquals("REST", metadata.inputInterfaceName());
            assertEquals("speilFysiskBrukerstedFraRessursTilOT", metadata.inputInterfaceMethodName());
            assertEquals("A000001", metadata.origination().initiatedBy());
            assertEquals("test-app", metadata.origination().initiatedFrom());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        void noActiveRessurser_returnsEmptyMessage() {
            when(ressursService.findAllActiveRessurser()).thenReturn(Collections.emptyMap());
            doReturn(Collections.emptyMap()).when(orgtilknytningService).getByNavidenter(any(), any());

            String dryResult = controller.speilFysiskBrukerstedFraRessursTilOt(true);
            assertTrue(dryResult.contains("0"));

            String liveResult = controller.speilFysiskBrukerstedFraRessursTilOt(false);
            assertEquals("", liveResult);
        }

        @Test
        void multipleOrgtilknytningerForOneRessurs_processesEach() {
            var rs = ressursWithPrimary( "1234");
            var ot1 = activeOt( "AB123C");
            var ot2 = activeOt( "CD456E");
            var ot3 = activeOt( "EF789G");

            when(ressursService.findAllActiveRessurser()).thenReturn(Map.of("A000001", rs));
            doReturn(Map.of("A000001", List.of(ot1, ot2, ot3))).when(orgtilknytningService).getByNavidenter(any(), any());

            controller.speilFysiskBrukerstedFraRessursTilOt(false);

            verify(orgtilknytningCommandService, times(3)).endreFysiskeBrukersted(any(EndreFysiskeBrukerstederCommand.class));
        }
    }
}
