package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// REF: no/nav/nom/apps/api/orgtilknytning/dvh/dokumentasjon/TestReferanser_ProsesserStillingPerioder.md
@DisplayName("Tester prosessering av strukturerte StillingPerioder fra DVH")
class NyeRessursStillingProcessorTest extends TestBase {
    NyeRessursStillingProcessor CUT;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CUT = new NyeRessursStillingProcessor(
            orgtilknytningService,
            orgtilknytningCommandService,
            ressursStateRepository,
            nomMastered,
            u4DiffPaaMastrede
        );

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_1", "FID_2", "FID_3", "FID_4", "FID_5");
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(RESSURS_A));
    }

    @Test
    @DisplayName("Ny stillingperiode uten eksisterende OT - Ref: 1.i")
    void ny_stillingperiode_uten_ot__1_i() {
        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, DD_6M_1D, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(DD_2Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_6M_1D)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(3, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("To stillingperioder uten eksisterende OT, overlapp og overtakende DOL - Ref: 1.ii")
    void to_stillingperioder_uten_ot__1_ii() {
        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, DD_6M_1D, DagligOppfolging.JA),
            stillingPeriode(ENHET_B, DD_1Y, DD_6M_1D, DagligOppfolging.NEI),
            stillingPeriode(ENHET_B, DD_6M, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        String fid1 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(DD_2Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid1)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_6M_1D)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid1)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
           .erDagligOppfolging(true);

        String fid2 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(DD_1Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid2)
            .gyldigFom(DD_1Y)
            .gyldigTom(DD_6M_1D)
            .getFid();

        String fid3 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(DD_6M)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid3)
            .forNavident(A000001)
            .tilEnhet(ENHET_B.getId())
            .erDagligOppfolging(true);

        assertEquals(3+2+2, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("En stillingperiode lik eksisterende OT - Ref: 2.i")
    void stillingperiode_lik_ot__2_i() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, ENHET_A.getId(), DD_2Y, DD_6M_1D, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, DD_6M_1D, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        verifyNoInteractions(orgtilknytningCommandService);
        assertEquals(0, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("En stillingperiode, eksisterende OT ulik DOL- Ref: 2.ii")
    void stillingperiode_ot_ulik_dol__2_ii() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_1", A000001, ENHET_A.getId(), DD_2Y, DD_6M_1D, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, DD_6M_1D, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid("FID_1")
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(1, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Single OT, single StillingPeriode ikke match på periode - Ref: 2.iii")
    void stillingperiode_ot_ikke_match_periode__2_iii() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_1", A000001, ENHET_A.getId(), DD_2Y, DD_6M_1D, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, DD_1Y_1D, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_1")
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid("FID_1")
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(2, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Single OT, single StillingPeriode ikke match på periode - Ref: 2.iv")
    void single_ot_single_stillingperiode_ikke_match_periode__2_iv() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_1", A000001, ENHET_A.getId(), DD_2Y, DD_6M_1D, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_1Y, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_1");

        String fid2 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(DD_1Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid2)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(3, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Single OT, to StillingPerioder, en match på periode - Ref: 2.v")
    void single_ot_to_stillingperioder_en_match_periode__2_v() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_1Y, null, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_B, DD_2Y, DD_1Y_1D, DagligOppfolging.JA),
            stillingPeriode(ENHET_A, DD_1Y, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        String fid2 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(DD_2Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid2)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid2)
            .forNavident(A000001)
            .tilEnhet(ENHET_B.getId())
            .erDagligOppfolging(true);

        assertEquals(3, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Single OT, to StillingPerioder, en delvis match på periode - Ref: 2.vi")
    void single_ot_to_stillingperioder_en_delvis_match_periode__2_vi() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_1Y, DD_6M_1D, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_B, DD_2Y, DD_1Y_1D, DagligOppfolging.JA),
            stillingPeriode(ENHET_A, DD_1Y, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        String fid_B =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(DD_2Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(DD_1Y)
            .gyldigTom(null);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_B)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid("FID_A")
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_B)
            .forNavident(A000001)
            .tilEnhet(ENHET_B.getId())
            .erDagligOppfolging(true);

        assertEquals(5, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Single OT, to StillingPerioder,ny tar over - Ref: 2.vii")
    void single_ot_to_stillingperioder_ny_tar_over__2_vii() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_2Y, null, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, DD_1Y_1D, DagligOppfolging.JA),
            stillingPeriode(ENHET_B, DD_1Y, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid("FID_A")
            .gyldigTom(DD_1Y_1D);

        String fid_B =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(DD_1Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_B)
            .forNavident(A000001)
            .tilEnhet(ENHET_B.getId())
            .erDagligOppfolging(true);

        assertEquals(3, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Single OT, to StillingPerioder,ny tar over fremtidig - Ref: 2.viii")
    void single_ot_to_stillingperioder_ny_tar_over_fremtidig__2_viii() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_2Y, null, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, SLUTT_OM_CA_2_MND, DagligOppfolging.JA),
            stillingPeriode(ENHET_B, START_OM_CA_2_MND, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid("FID_A")
            .gyldigTom(SLUTT_OM_CA_2_MND);

        String fid_B =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(START_OM_CA_2_MND)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_B)
            .forNavident(A000001)
            .tilEnhet(ENHET_B.getId())
            .erDagligOppfolging(true);

        assertEquals(3, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Flere OTer, flere StillingPerioder, en match en delvis match - Ref: 2.ix")
    void flere_ot_flere_stillingperioder_en_match_en_delvis__2_ix() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_2Y, DD_1Y_1D, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_2Y, DD_6M_1D, false),
            orgtilknytning("FID_C", A000001, ENHET_C.getId(), DD_1Y, SLUTT_OM_CA_2_MND, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_2Y, DD_1Y_1D, DagligOppfolging.JA),
            stillingPeriode(ENHET_C, DD_1Y, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_B");

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_C")
            .gyldigFom(DD_1Y)
            .gyldigTom(null);

        assertEquals(2, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Flere OTer, flere StillingPerioder, en match en delvis match - Ref: 2.x")
    void flere_ot_flere_stillingperioder_en_match_en_delvis__2_x() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_2Y, DD_1Y_1D, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_2Y, DD_6M_1D, true),
            orgtilknytning("FID_C", A000001, ENHET_C.getId(), DD_6M, null, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_B, DD_2Y, DD_1Y_1D, DagligOppfolging.JA),
            stillingPeriode(ENHET_C, DD_1Y, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_A");

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_B")
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_C");

        String ny_fid_C =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_C.getId())
                .gyldigFom(DD_1Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(ny_fid_C)
            .forNavident(A000001)
            .tilEnhet(ENHET_C.getId())
            .erDagligOppfolging(true);

        assertEquals(5, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Flere OTer en historisk, flere StillingPerioder en klippet i historikk, behold gammel historikk - Ref: 2.xi")
    void flere_ot_flere_stillingperioder_match_gammel_historisk__2_xi() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y2M, DD_2Y_1D, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_2Y, null, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, DD_3Y, DD_2Y_1D, DagligOppfolging.JA),
            stillingPeriode(ENHET_B, DD_2Y, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        verifyNoInteractions(orgtilknytningCommandService);
        assertEquals(0, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("En OTer historisk slutt +3år siden, ingen StillingPerioder (klippet historikk), behold gammel historikk - Ref: 2.xii")
    void en_historisk_ot_ingen_sp_behold_gammel_historisk__2_xii() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_5Y, DD_4Y, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of());

        var sspForNavidentResult = CUT.process(inputToProcess);

        verifyNoInteractions(orgtilknytningCommandService);
        assertEquals(0, sspForNavidentResult.get(A000001).endringer().size());
    }

    // ------ Helpers

    private Map<String, StrukturerteStillingPerioderForNavident> getInputToProcess(String navident, List<StillingPeriode> stillingPerioder) {
        return Map.of(navident,
            sspForNavident(
                A000001,
                stillingPerioder
        ));
    }

    private StrukturerteStillingPerioderForNavident sspForNavident(String navident, List<StillingPeriode> ressursStillinger) {
        return new StrukturerteStillingPerioderForNavident(navident, ressursStillinger, Collections.emptyList());
    }

    static final String A000001 = "A000001";
    final RessursState RESSURS_A = ressurs(A000001);

    final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);
    final LocalDate DD_5Y = LocalDate.now().minusYears(5).withDayOfMonth(1); // 5 år siden
    final LocalDate DD_4Y = LocalDate.now().minusYears(4).withDayOfMonth(1); // 4 år siden
    final LocalDate DD_3Y2M = LocalDate.now().minusYears(3).minusMonths(2).withDayOfMonth(1); // 3 år, 2 måneder siden
    final LocalDate DD_3Y = LocalDate.now().minusYears(3).withDayOfMonth(1); // 3 år siden
    final LocalDate DD_2Y = LocalDate.now().minusYears(2).withDayOfMonth(1); // 2 år siden
    final LocalDate DD_2Y_1D = DD_2Y.minusDays(1);                           // Dagen før 1 år siden
    final LocalDate DD_1Y = LocalDate.now().minusYears(1).withDayOfMonth(1); // 1 år siden
    final LocalDate DD_1Y_1D = DD_1Y.minusDays(1);                           // Dagen før 1 år siden
    final LocalDate DD_6M = LocalDate.now().minusMonths(6).withDayOfMonth(1); // 6 måneder siden
    final LocalDate DD_6M_1D = DD_6M.minusDays(1);                            // Dagen før 6 måneder siden

    final LocalDate START_OM_CA_2_MND = LocalDate.now().plusMonths(3).withDayOfMonth(1);
    final LocalDate SLUTT_OM_CA_2_MND = START_OM_CA_2_MND.minusDays(1);

    IOrgenhet ENHET_A = new OrgEnhet(
        new OrgEnhetState(
            "FID_A", "a100xy", "Enhet A",
            LocalDate.now().minusYears(10), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("Enhet_A"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_B = new OrgEnhet(
        new OrgEnhetState(
            "FID_B", "b100xy", "Enhet B",
            LocalDate.now().minusYears(10), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("Enhet_B"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_C = new OrgEnhet(
        new OrgEnhetState(
            "FID_C", "c100xy", "Enhet C",
            LocalDate.now().minusYears(10), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("Enhet_C"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
}