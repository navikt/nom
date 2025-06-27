package no.nav.nom.apps.api.ressurs.api.admin;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.contract.nom.Sektor.EKSTERN;
import static no.nav.nom.contract.nom.Sektor.NAV_KOMMUNAL;
import static no.nav.nom.contract.nom.Sektor.NAV_STATLIG;
import static no.nav.nom.contract.nom.Tilknytningtype.FAST;
import static no.nav.nom.contract.nom.Tilknytningtype.MIDLERTIDIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {MastreStatligeINom.class})
class MastreStatligeINomTest {
    @MockitoBean
    RessursStateRepository ressursStateRepository;
    @MockitoBean
    RessursCommandService ressursCommandService;
    @MockitoBean
    OrgtilknytningService orgtilknytningService;
    @MockitoBean
    NomMastered nomMastered;
    @Autowired
    MastreStatligeINom mastreStatligeINom;
    @Captor
    ArgumentCaptor<OppdaterSluttdatoCommand> oppdaterSluttdatoCommandCaptor;

    LocalDate today = LocalDate.now();
    LocalDate dd5uker = today.plusWeeks(5);
    LocalDate dd1aar = today.plusYears(1);

    @Test
    @DisplayName("ekskludering av navidenter ved mastring")
    void ekskluderingAvNavidenterTest() {
        when(ressursStateRepository.getAll()).thenReturn(List.of(
            getRessursState("A000001", NAV_STATLIG),
            getRessursState("A000002", NAV_STATLIG),
            getRessursState("A000003", NAV_KOMMUNAL),
            getRessursState("A000004", NAV_STATLIG),
            getRessursState("A000005", EKSTERN),
            getRessursState("A000006", NAV_STATLIG),
            getRessursState("A000007", NAV_STATLIG)
        ));

        when(nomMastered.isMastered(any())).thenReturn(false);

        var res = mastreStatligeINom.markerStatligeSomMastret(true, List.of("A000001", "A000006"));

        verify(nomMastered, times(5)).isMastered(any());
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000002");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000004");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000007");

        log.info(res);
    }

    @Test
    @DisplayName("ekskludering av navidenter ved mastring gjennom controller metode")
    void ekskluderingAvNavidenterGjennomControllerMetodeTest() {
        when(ressursStateRepository.getAll()).thenReturn(List.of(
            getRessursState("A000001", NAV_STATLIG),
            getRessursState("A000002", NAV_STATLIG),
            getRessursState("A000003", NAV_KOMMUNAL),
            getRessursState("A000004", NAV_STATLIG),
            getRessursState("A000005", EKSTERN),
            getRessursState("A000006", NAV_STATLIG),
            getRessursState("A000007", NAV_STATLIG)
        ));

        when(nomMastered.isMastered(any())).thenReturn(false);

        var res = mastreStatligeINom.markerStatligeSomMastret("Confirm", List.of("A000001", "A000006"));

        verify(nomMastered, times(5)).isMastered(any());
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000002");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000004");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000007");

        log.info(res.toString());
    }


    @Test
    @DisplayName("Sett mastret commit = false")
    void settMastretCommitFalseTest() {
        when(ressursStateRepository.getAll()).thenReturn(List.of(
            getRessursState("A000001", NAV_STATLIG),
            getRessursState("A000002", NAV_STATLIG),
            getRessursState("A000003", NAV_KOMMUNAL),
            getRessursState("A000004", NAV_STATLIG),
            getRessursState("A000005", EKSTERN),
            getRessursState("A000006", NAV_STATLIG),
            getRessursState("A000007", NAV_STATLIG)
        ));

        when(nomMastered.isMastered(any())).thenReturn(false);

        var res = mastreStatligeINom.markerStatligeSomMastret(false, List.of("A000001", "A000006"));

        verify(nomMastered, times(5)).isMastered(any());
        verify(nomMastered, times(0)).markRessursAsMasteredInNom("A000002");
        verify(nomMastered, times(0)).markRessursAsMasteredInNom("A000004");
        verify(nomMastered, times(0)).markRessursAsMasteredInNom("A000007");

        log.info(res);
    }

    @Test
    @DisplayName("Oppdater ressurs sluttdato")
    void oppdaterRessursSluttdatoTest() {
        var list = List.of(
            new MastreStatligeINom.Ressurs("FID_1", "A0000001", NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2023-12-31")),
            new MastreStatligeINom.Ressurs("FID_2", "A0000002", NAV_STATLIG, LocalDate.parse("2023-01-01"), null, LocalDate.parse("2024-07-01")),
            new MastreStatligeINom.Ressurs("FID_3", "A0000003", NAV_STATLIG, LocalDate.parse("2023-01-01"), null, LocalDate.parse("2099-12-31"))
        );

        var output = mastreStatligeINom.oppdaterRessursSluttdato(true, list, null);

        verify(ressursCommandService, times(3)).oppdaterRessursSluttdatoEvent(oppdaterSluttdatoCommandCaptor.capture());

        var res1 = oppdaterSluttdatoCommandCaptor.getAllValues().get(0);
        var res2 = oppdaterSluttdatoCommandCaptor.getAllValues().get(1);
        var res3 = oppdaterSluttdatoCommandCaptor.getAllValues().get(2);

        assertEquals("FID_1", res1.fid());
        assertEquals(LocalDate.parse("2023-12-31"), res1.sluttdato().orElse(null));

        assertEquals("FID_2", res2.fid());
        assertEquals(LocalDate.parse("2024-07-01"), res2.sluttdato().orElse(null));

        assertEquals("FID_3", res3.fid());
        assertNull(res3.sluttdato().orElse(null));

        log.info(output);
    }

    @Test
    @DisplayName("Test utvalg gruppeA")
    void gruppeAUtvalgTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.utvalgGruppeA(dd5uker);

        assertEquals(3, res.size());
    }

    @Test
    @DisplayName("Test utvalg gruppeB")
    void gruppeBUtvalgTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.utvalgGruppeB(dd5uker, LocalDate.parse("2024-12-01"));

        assertEquals(6, res.size());
    }

    @Test
    @DisplayName("Test utvalg gruppeC")
    void gruppeCUtvalgTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.utvalgGruppeC(dd5uker, LocalDate.parse("2024-12-31"));

        assertEquals(2, res.size());
    }

    @Test
    @DisplayName("Test oppdater gruppeA")
    void OppdaterGruppeATest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.oppdaterRessursSluttdatoGrpA(dd5uker, List.of("A000001", "A000002", "A000003"), true, "USERID");

        assertTrue(res.getBody().contains("Antall navidenter å ekskludere: 3"));
        assertTrue(res.getBody().contains("Antall i Gruppe A utvalg: 3"));
        assertTrue(res.getBody().contains("Antall å oppdatere: 3"));

        verify(ressursCommandService, times(3)).oppdaterRessursSluttdatoEvent(any());
    }

    @Test
    @DisplayName("Test oppdater gruppeA - DRY RUN")
    void OppdaterGruppeADryRunTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.oppdaterRessursSluttdatoGrpA(dd5uker, List.of("A000001", "A000002", "A000003"), false, "USERID");

        assertTrue(res.getBody().contains("DRY RUN"));
        assertTrue(res.getBody().contains("Antall navidenter å ekskludere: 3"));
        assertTrue(res.getBody().contains("Antall i Gruppe A utvalg: 3"));
        assertTrue(res.getBody().contains("Antall å oppdatere: 3"));

        verifyNoInteractions(ressursCommandService);
    }

    @Test
    @DisplayName("Test oppdater gruppeB")
    void OppdaterGruppeBTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.oppdaterRessursSluttdatoGrpB(dd5uker, LocalDate.parse("2024-12-01"), List.of("A000001", "A000018", "A000019"), true, "USERID");

        assertTrue(res.getBody().contains("Antall navidenter å ekskludere: 3"));
        assertTrue(res.getBody().contains("Antall i Gruppe B utvalg: 6"));
        assertTrue(res.getBody().contains("Antall å oppdatere: 5"));

        verify(ressursCommandService, times(5)).oppdaterRessursSluttdatoEvent(any());
    }

    @Test
    @DisplayName("Test oppdater gruppeB - DRY RUN")
    void OppdaterGruppeBDryRunTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.oppdaterRessursSluttdatoGrpB(dd5uker, LocalDate.parse("2024-12-01"), List.of("A000001", "A000018", "A000019"), false, "USERID");

        assertTrue(res.getBody().contains("DRY RUN"));
        assertTrue(res.getBody().contains("Antall navidenter å ekskludere: 3"));
        assertTrue(res.getBody().contains("Antall i Gruppe B utvalg: 6"));
        assertTrue(res.getBody().contains("Antall å oppdatere: 5"));

        verifyNoInteractions(ressursCommandService);
    }

    @Test
    @DisplayName("Test oppdater gruppeC")
    void OppdaterGruppeCTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.oppdaterRessursSluttdatoGrpC(dd5uker, LocalDate.parse("2024-12-31"), List.of("A000001", "A000018", "A000019"), false, true, "USERID");

        assertTrue(res.getBody().contains("Antall navidenter å ekskludere: 3"));
        assertTrue(res.getBody().contains("Antall i Gruppe C utvalg: 2"));
        assertTrue(res.getBody().contains("Antall å oppdatere: 2"));

        verify(ressursCommandService, times(2)).oppdaterRessursSluttdatoEvent(any());
    }

    @Test
    @DisplayName("Test oppdater gruppeC- DRY RUN")
    void OppdaterGruppeCDryRunTest() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.oppdaterRessursSluttdatoGrpC(dd5uker, LocalDate.parse("2024-12-31"), List.of("A000001", "A000018", "A000019"), false, false, "USERID");

        assertTrue(res.getBody().contains("DRY RUN"));
        assertTrue(res.getBody().contains("Antall navidenter å ekskludere: 3"));
        assertTrue(res.getBody().contains("Antall i Gruppe C utvalg: 2"));
        assertTrue(res.getBody().contains("Antall å oppdatere: 2"));

        verifyNoInteractions(ressursCommandService);
    }

    @Test
    @DisplayName("Test oppdater gruppeC - begrens utvalg")
    void OppdaterGruppeCTestBegrensUtvalg() {
        createStatesForUtvalgTests();

        var res = mastreStatligeINom.oppdaterRessursSluttdatoGrpC(dd5uker, LocalDate.parse("2024-12-31"), List.of("A000006"), true, true, "USERID");

        assertTrue(res.getBody().contains("Antall navidenter å begrense utvalget med: 1"));
        assertTrue(res.getBody().contains("Antall i Gruppe C utvalg: 2"));
        assertTrue(res.getBody().contains("Antall å oppdatere: 1"));

        verify(ressursCommandService, times(1)).oppdaterRessursSluttdatoEvent(any());
    }

    @Test
    @DisplayName("getRessursRow output")
    void getRessursRowTest() {
        var list = List.of(
            new MastreStatligeINom.Ressurs("FID_1", "123456789", NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01")),
            new MastreStatligeINom.Ressurs("FID_2", "123456789", NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01")),
            new MastreStatligeINom.Ressurs("FID_3", "123456789", NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01"))
        );

        StringBuffer sb = new StringBuffer();
        list.forEach(MastreStatligeINom.getRessursRow(sb));

        log.info("Resultat: {}", sb);
        assertEquals("123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n", sb.toString());
    }

    private RessursState getRessursState(String navident, Sektor sektor) {
        return RessursState.builder()
            .navident(navident)
            .sektor(sektor)
            .build();
    }

    private void createStatesForUtvalgTests() {
        when(ressursStateRepository.getAll()).thenReturn(List.of(
            getRessursState("A000001", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               // A
            getRessursState("A000002", NAV_STATLIG, LocalDate.parse("2021-01-01"), LocalDate.parse("2023-12-31")),      //
            getRessursState("A000003", NAV_STATLIG, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-11-20")),      //
            getRessursState("A000004", NAV_STATLIG, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-12-31")),      //
            getRessursState("A000005", NAV_STATLIG, LocalDate.parse("2021-01-01"), LocalDate.parse("2025-02-28")),      //      C
            getRessursState("A000006", NAV_STATLIG, LocalDate.parse("2021-01-01"), dd5uker),                            //      C
            getRessursState("A000007", NAV_STATLIG, LocalDate.parse("2024-12-01"), null),                               // A
            getRessursState("A000008", NAV_STATLIG, LocalDate.parse("2024-12-01"), null),                               // -
            getRessursState("A000009", EKSTERN, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31")),
            getRessursState("A000010", NAV_KOMMUNAL, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31")),
            getRessursState("A000011", EKSTERN, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31")),
            getRessursState("A000012", NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2025-02-28")),      // -
            getRessursState("A000013", NAV_STATLIG, LocalDate.parse("2023-01-01"), null),                               // -
            getRessursState("A000014", NAV_STATLIG, today, null),                                                       // A
            getRessursState("A000015", NAV_STATLIG, dd5uker, null),                                                     // A
            getRessursState("A000016", NAV_STATLIG, dd5uker.plusDays(10), null),                                        // A
            getRessursState("A000017", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //
            getRessursState("A000018", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //
            getRessursState("A000019", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //   B
            getRessursState("A000020", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //   B
            getRessursState("A000021", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //   B
            getRessursState("A000022", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //   B
            getRessursState("A000023", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //   B
            getRessursState("A000024", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               //   B
            getRessursState("A000025", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               // A
            getRessursState("A000026", NAV_STATLIG, LocalDate.parse("2021-01-01"), null),                               // A
            getRessursState("A000027", NAV_STATLIG, LocalDate.parse("2021-01-01"), null)                                // A
        ));

        when(nomMastered.isMastered("A000001")).thenReturn(true);
        when(nomMastered.isMastered("A000002")).thenReturn(true);
        when(nomMastered.isMastered("A000002")).thenReturn(true);
        when(nomMastered.isMastered("A000003")).thenReturn(true);
        when(nomMastered.isMastered("A000004")).thenReturn(true);
        when(nomMastered.isMastered("A000005")).thenReturn(true);
        when(nomMastered.isMastered("A000006")).thenReturn(true);
        when(nomMastered.isMastered("A000007")).thenReturn(true);
        when(nomMastered.isMastered("A000008")).thenReturn(false);
        when(nomMastered.isMastered("A000011")).thenReturn(true);
        when(nomMastered.isMastered("A000010")).thenReturn(true);
        when(nomMastered.isMastered("A000009")).thenReturn(true);
        when(nomMastered.isMastered("A000012")).thenReturn(false);
        when(nomMastered.isMastered("A000013")).thenReturn(false);
        when(nomMastered.isMastered("A000014")).thenReturn(true);
        when(nomMastered.isMastered("A000015")).thenReturn(true);
        when(nomMastered.isMastered("A000016")).thenReturn(true);
        when(nomMastered.isMastered("A000017")).thenReturn(true);
        when(nomMastered.isMastered("A000018")).thenReturn(true);
        when(nomMastered.isMastered("A000019")).thenReturn(true);
        when(nomMastered.isMastered("A000020")).thenReturn(true);
        when(nomMastered.isMastered("A000021")).thenReturn(true);
        when(nomMastered.isMastered("A000022")).thenReturn(true);
        when(nomMastered.isMastered("A000023")).thenReturn(true);
        when(nomMastered.isMastered("A000024")).thenReturn(true);
        when(nomMastered.isMastered("A000025")).thenReturn(true);
        when(nomMastered.isMastered("A000026")).thenReturn(true);
        when(nomMastered.isMastered("A000027")).thenReturn(true);

        when(orgtilknytningService.getByNavIdent("A000001")).thenReturn(List.of(
            getOTState("A000001", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), null)
        ));

        when(orgtilknytningService.getByNavIdent("A000001")).thenReturn(List.of(
            getOTState("A000001", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2023-12-31")),
            getOTState("A000001", "c321ab", NAV_STATLIG, MIDLERTIDIG, false, LocalDate.parse("2022-01-01"), LocalDate.parse("2023-12-31")),
            getOTState("A000001", "x789yz", NAV_STATLIG, FAST, true, LocalDate.parse("2024-01-01"), null)
        ));
        when(orgtilknytningService.getByNavIdent("A000002")).thenReturn(List.of(
            getOTState("A000002", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2023-12-31"))
        ));
        when(orgtilknytningService.getByNavIdent("A000003")).thenReturn(List.of(
            getOTState("A000003", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-11-20"))
        ));
        when(orgtilknytningService.getByNavIdent("A000004")).thenReturn(List.of(
            getOTState("A000004", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-12-31"))
        ));
        when(orgtilknytningService.getByNavIdent("A000005")).thenReturn(List.of(
            getOTState("A000005", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2025-02-28"))
        ));
        when(orgtilknytningService.getByNavIdent("A000006")).thenReturn(List.of(
            getOTState("A000006", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), dd5uker)
        ));
        when(orgtilknytningService.getByNavIdent("A000007")).thenReturn(List.of(
            getOTState("A000007", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2024-12-01"), null)
        ));
        when(orgtilknytningService.getByNavIdent("A000008")).thenReturn(List.of(
            getOTState("A000008", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2024-12-01"), null)
        ));
        when(orgtilknytningService.getByNavIdent("A000009")).thenReturn(List.of(
            getOTState("A000009", "a123bc", EKSTERN, FAST, true, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"))
        ));
        when(orgtilknytningService.getByNavIdent("A000010")).thenReturn(List.of(
            getOTState("A000010", "a123bc", NAV_KOMMUNAL, FAST, true, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"))
        ));
        when(orgtilknytningService.getByNavIdent("A000011")).thenReturn(List.of(
            getOTState("A000011", "a123bc", EKSTERN, FAST, true, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"))
        ));
        when(orgtilknytningService.getByNavIdent("A000012")).thenReturn(List.of(
            getOTState("A000012", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2023-01-01"), LocalDate.parse("2025-02-28"))
        ));
        when(orgtilknytningService.getByNavIdent("A000013")).thenReturn(List.of(
            getOTState("A000013", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2023-01-01"), null)
        ));
        when(orgtilknytningService.getByNavIdent("A000014")).thenReturn(List.of(
            getOTState("A000014", "a123bc", NAV_STATLIG, FAST, true, today, null)
        ));
        when(orgtilknytningService.getByNavIdent("A000015")).thenReturn(List.of(
            getOTState("A000015", "a123bc", NAV_STATLIG, FAST, true, dd5uker, null)
        ));
        when(orgtilknytningService.getByNavIdent("A000016")).thenReturn(List.of(
            getOTState("A000016", "a123bc", NAV_STATLIG, FAST, true, dd5uker.plusDays(10), null)
        ));
        when(orgtilknytningService.getByNavIdent("A000017")).thenReturn(List.of(
            getOTState("A000017", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-11-30"))
        ));
        when(orgtilknytningService.getByNavIdent("A000018")).thenReturn(List.of(
            getOTState("A000018", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-11-30"))
        ));
        when(orgtilknytningService.getByNavIdent("A000019")).thenReturn(List.of(
            getOTState("A000019", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-12-01"))
        ));
        when(orgtilknytningService.getByNavIdent("A000020")).thenReturn(List.of(
            getOTState("A000020", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2024-12-31"))
        ));
        when(orgtilknytningService.getByNavIdent("A000021")).thenReturn(List.of(
            getOTState("A000021", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), LocalDate.parse("2025-01-01"))
        ));
        when(orgtilknytningService.getByNavIdent("A000022")).thenReturn(List.of(
            getOTState("A000022", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), today)
        ));
        when(orgtilknytningService.getByNavIdent("A000023")).thenReturn(List.of(
            getOTState("A000023", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), dd5uker)
        ));
        when(orgtilknytningService.getByNavIdent("A000024")).thenReturn(List.of(
            getOTState("A000024", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), dd5uker.minusDays(1))
        ));
        when(orgtilknytningService.getByNavIdent("A000025")).thenReturn(List.of(
            getOTState("A000025", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), dd5uker.plusDays(1))
        ));
        when(orgtilknytningService.getByNavIdent("A000026")).thenReturn(List.of(
            getOTState("A000026", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), dd5uker.plusWeeks(1))
        ));
        when(orgtilknytningService.getByNavIdent("A000027")).thenReturn(List.of(
            getOTState("A000027", "a123bc", NAV_STATLIG, FAST, true, LocalDate.parse("2021-01-01"), dd1aar)
        ));


    }

    private RessursState getRessursState(String navident, Sektor sektor, LocalDate startdato, LocalDate sluttdato) {
        return RessursState.builder()
            .navident(navident)
            .sektor(sektor)
            .startdato(startdato == null ? Optional.empty() : Optional.of(startdato))
            .sluttdato(sluttdato == null ? Optional.empty() : Optional.of(sluttdato))
            .build();
    }

    private OrgtilknytningState getOTState(String navident, String orgEnhetId, Sektor sektor, Tilknytningtype tilknytningtype, boolean dol, LocalDate gyldigFom, LocalDate gyldigTom) {
        return OrgtilknytningState.builder()
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .sektor(sektor)
            .orgtilknytningType(tilknytningtype == null ? Optional.empty() : Optional.of(tilknytningtype))
            .erDagligOppfolging(dol)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom == null ? Optional.empty() : Optional.of(gyldigTom))
            .build();
    }
}