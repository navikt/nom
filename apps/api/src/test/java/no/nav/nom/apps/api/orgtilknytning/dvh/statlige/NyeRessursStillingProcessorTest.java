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

import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.NyeRessursStillingProcessor.LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
            unleash
        );

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_1", "FID_2", "FID_3", "FID_4", "FID_5");
        when(unleash.isEnabled(eq(LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST), anyBoolean())).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(RESSURS_A));
    }

    @Test
    @DisplayName("Ny stillingperiode uten eksisterende OT - Ref: 1.i")
    void ny_stillingperiode_uten_ot__1_i() {
        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2022_01_01, _2024_01_31, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(_2022_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid)
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2024_01_31)
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
            stillingPeriode(ENHET_A, _2022_01_01, _2024_01_31, DagligOppfolging.JA),
            stillingPeriode(ENHET_B, _2023_01_01, _2024_01_31, DagligOppfolging.NEI),
            stillingPeriode(ENHET_B, _2024_02_01, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        String fid1 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(_2022_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid1)
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2024_01_31)
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
                .gyldigFom(_2023_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid2)
            .gyldigFom(_2023_01_01)
            .gyldigTom(_2024_01_31)
            .getFid();

        String fid3 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(_2024_02_01)
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
            orgtilknytning(A000001, ENHET_A.getId(), _2022_01_01, _2024_01_31, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2022_01_01, _2024_01_31, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        verifyNoInteractions(orgtilknytningCommandService);
        assertEquals(0, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("En stillingperiode, eksisterende OT ulik DOL- Ref: 2.ii")
    void stillingperiode_ot_ulik_dol__2_ii() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_1", A000001, ENHET_A.getId(), _2022_01_01, _2024_01_31, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2022_01_01, _2024_01_31, DagligOppfolging.JA)
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
            orgtilknytning("FID_1", A000001, ENHET_A.getId(), _2022_01_01, _2024_01_31, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2022_01_01, _2022_12_31, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_1")
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2022_12_31);

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
            orgtilknytning("FID_1", A000001, ENHET_A.getId(), _2022_01_01, _2024_01_31, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2023_01_01, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_1");

        String fid2 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(_2023_01_01)
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
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2023_01_01, null, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_B, _2022_01_01, _2022_12_31, DagligOppfolging.JA),
            stillingPeriode(ENHET_A, _2023_01_01, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        String fid2 =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(_2022_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid2)
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2022_12_31);

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
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2023_01_01, _2024_01_31, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_B, _2022_01_01, _2022_12_31, DagligOppfolging.JA),
            stillingPeriode(ENHET_A, _2023_01_01, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        String fid_B =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(_2022_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(_2023_01_01)
            .gyldigTom(null);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_B)
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2022_12_31);

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
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2022_01_01, null, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2022_01_01, _2022_12_31, DagligOppfolging.JA),
            stillingPeriode(ENHET_B, _2023_01_01, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid("FID_A")
            .gyldigTom(_2022_12_31);

        String fid_B =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_B.getId())
                .gyldigFom(_2023_01_01)
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
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2022_01_01, null, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2022_01_01, SLUTT_OM_CA_2_MND, DagligOppfolging.JA),
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
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2022_01_01, _2022_12_31, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), _2022_01_01, _2024_01_31, false),
            orgtilknytning("FID_C", A000001, ENHET_C.getId(), _2023_01_01, SLUTT_OM_CA_2_MND, true)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_A, _2022_01_01, _2022_12_31, DagligOppfolging.JA),
            stillingPeriode(ENHET_C, _2023_01_01, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);


        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_B");

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_C")
            .gyldigFom(_2023_01_01)
            .gyldigTom(null);

        assertEquals(2, sspForNavidentResult.get(A000001).endringer().size());
    }

    @Test
    @DisplayName("Flere OTer, flere StillingPerioder, en match en delvis match - Ref: 2.x")
    void flere_ot_flere_stillingperioder_en_match_en_delvis__2_x() {
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2022_01_01, _2022_12_31, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), _2022_01_01, _2024_01_31, true),
            orgtilknytning("FID_C", A000001, ENHET_C.getId(), _2024_02_01, null, false)
        ));

        var inputToProcess = getInputToProcess(A000001, List.of(
            stillingPeriode(ENHET_B, _2022_01_01, _2022_12_31, DagligOppfolging.JA),
            stillingPeriode(ENHET_C, _2023_01_01, NO_END_DATE, DagligOppfolging.JA)
        ));

        var sspForNavidentResult = CUT.process(inputToProcess);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_A");

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_B")
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2022_12_31);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_C");

        String ny_fid_C =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(A000001)
                .tilEnhet(ENHET_C.getId())
                .gyldigFom(_2023_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(ny_fid_C)
            .forNavident(A000001)
            .tilEnhet(ENHET_C.getId())
            .erDagligOppfolging(true);

        assertEquals(5, sspForNavidentResult.get(A000001).endringer().size());
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
    final LocalDate _2022_01_01 = LocalDate.of(2021, 1, 1);
    final LocalDate _2022_12_31 = LocalDate.of(2022, 12, 31);
    final LocalDate _2023_01_01 = LocalDate.of(2023, 1, 1);
    final LocalDate _2024_01_31 = LocalDate.of(2024, 1, 31);
    final LocalDate _2024_02_01 = LocalDate.of(2024, 2, 1);

    final LocalDate START_OM_CA_2_MND = LocalDate.now().plusMonths(3).withDayOfMonth(1);
    final LocalDate SLUTT_OM_CA_2_MND = START_OM_CA_2_MND.minusDays(1);

    IOrgenhet ENHET_A = new OrgEnhet(
        new OrgEnhetState(
            "FID_A", "a100xy", "Enhet A",
            LocalDate.of(2015, 1, 1), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("Enhet_A"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_B = new OrgEnhet(
        new OrgEnhetState(
            "FID_B", "b100xy", "Enhet B",
            LocalDate.of(2015, 1, 1), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("Enhet_B"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_C = new OrgEnhet(
        new OrgEnhetState(
            "FID_C", "c100xy", "Enhet C",
            LocalDate.of(2015, 1, 1), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("Enhet_C"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
}