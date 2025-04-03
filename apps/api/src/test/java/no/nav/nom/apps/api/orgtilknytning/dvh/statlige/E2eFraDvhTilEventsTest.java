package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

// REF: no/nav/nom/apps/api/orgtilknytning/dvh/dokumentasjon/TestReferanser_E2E_Fra_Dvh_til_events.md
@DisplayName("Tester prosessering av strukturerte StillingPerioder fra DVH")
class E2eFraDvhTilEventsTest extends TestBase {
    NyeRessursStillingStrukturerer strukturer;
    NyeRessursStillingProcessor prosserer;

    @Autowired
    Unleash unleash;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("En DvhStilling, ingen eksisterende OT - Ref: 1")
    void e2e_test_1(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_3Y, DD_1Y_1D)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid = assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(RESSURS_1.navident())
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(DD_3Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(1, result.size());
        assertEquals(3, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("To sammenhengende DvhStillinger, ingen eksisterende OT - Ref: 2")
    void e2e_test_2(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_1Y, NO_END_DATE)
        );
        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid = assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(RESSURS_1.navident())
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(DD_3Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(1, result.size());
        assertEquals(2, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("To sammenhengende DvhStillinger, matcher eksisterende OT - Ref: 3")
    void e2e_test_3(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_1Y, NO_END_DATE)
        );
        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, null, true)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertEquals(1, result.size());
        assertEquals(0, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("En DvhStilling, matcher med agressoId mot to Enheter - Ref: 4")
    void e2e_test_4(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2", DD_3Y, NO_END_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(DD_3Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);


        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(DD_1Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .erDagligOppfolging(true);

        assertEquals(1, result.size());
        assertEquals(5, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("To DvhStillinger, en matcher med agressoId mot to Enheter, en matcher ikke Orgenhet På Ressurs, ulike OTer - Ref: 5")
    void e2e_test_5(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_1Y, DD_6M_1D),
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2","0DD0001", DD_2Y, NO_END_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, DD_1Y_1D, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_1Y, null, true)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_2Y_1D);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a2)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_6M_1D);

        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(DD_1Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_B");

        assertEquals(1, result.size());
        assertEquals(9, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("Tre DvhStillinger, to med samme sektor, en uten, men de to med sektor har forskjellige tilknytningtyper - Ref: 6")
    void e2e_test_6() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_1Y, DD_6M_1D, Optional.of("Midlertidig karakter § 9a")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A","0AA0001", DD_6M, NO_END_DATE, Optional.of("Fast"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid_a1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_3Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a1)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_1Y_1D);

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_1Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a2)
            .gyldigFom(DD_1Y)
            .gyldigTom(DD_6M_1D);

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_a2)
            .tilTilknytningtype(Optional.of(Tilknytningtype.MIDLERTIDIG));

        String fid_a3 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_6M)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a3)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid(fid_a3)
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_A.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_a3)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        assertEquals(1, result.size());
        assertEquals(11, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("To DvhStillinger, en matcher med agressoId mot to Enheter, en matcher ikke Orgenhet På Ressurs, ulike OTer, like sektor/tilknytning - Ref: 7")
    void e2e_test_7() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_3Y, DD_1Y_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_1Y, DD_6M_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2","0DD0001", DD_2Y, NO_END_DATE, Optional.of("Fast"))
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, DD_1Y_1D, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_1Y, null, true, Sektor.NAV_STATLIG, Tilknytningtype.MIDLERTIDIG)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_2Y_1D);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a2)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_6M_1D);

        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(DD_1Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_D2.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_d2)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_B");

        assertEquals(1, result.size());
        assertEquals(11, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("Test case 1 fra produksjon, hvor DVH lasten skal rette opp en feil produsert med flere DOL. Se JIRA: STS-37536 og JIRA: ORG-2450 - Ref: 8")
    void e2e_test_8() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", LocalDate.of(2022, 1, 1), LocalDate.of(2023, 4, 30), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 11, 30), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", LocalDate.of(2023, 12, 1), LocalDate.of(2024, 4, 30), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001",LocalDate.of(2024, 5, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0BB0001", "Enhet B", "0BB0001", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), Optional.of("Fungering")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A","0BB0001", LocalDate.of(2026, 1, 1), MAX_DATE, Optional.of("Fast"))
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), LocalDate.of(2021, 5, 1), null, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST),
            orgtilknytning("FID_A1", A000001, ENHET_A.getId(), LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31), true, null, null),
            orgtilknytning("FID_A2", A000001, ENHET_A.getId(), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), false, null, null),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), true, null, null)
        ));
        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid("FID_A").gyldigTom(LocalDate.of(2024, 12, 31));

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid("FID_A2")
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_A.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid("FID_A2")
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        String fid_a3 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(LocalDate.of(2026, 1, 1))
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a3)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid(fid_a3)
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_A.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_a3)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_A1");

        assertEquals(1, result.size());
        assertEquals(8, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("Test case 2 fra produksjon, hvor DVH lasten skal rette opp en feil produsert med flere DOL. Se JIRA: STS-37536 og JIRA: ORG-2450 - Ref: 9")
    void e2e_test_9() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", LocalDate.of(2022, 1, 1), LocalDate.of(2023, 4, 30), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 11, 30), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", LocalDate.of(2023, 12, 1), LocalDate.of(2024, 4, 30), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001",LocalDate.of(2024, 5, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 5, 31), Optional.of("Fungering")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A","0AA0001", LocalDate.of(2026, 1, 1), MAX_DATE, Optional.of("Fast"))
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), LocalDate.of(2021, 5, 1), null, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST),
            orgtilknytning("FID_A2", A000001, ENHET_A.getId(), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), false, null, null)
        ));
        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_A2")
            .gyldigFom(LocalDate.of(2025, 1, 1))
            .gyldigTom(LocalDate.of(2025, 12, 31));

        assertEquals(1, result.size());
        assertEquals(1, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("To dvhStillinger som overlapper hverandre med A tilknytning, som blir 3 stillingperioder, hvor vi ender opp med 3 OT - Ref: 10")
    void e2e_test_10() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_2Y, MAX_DATE, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_3Y, MAX_DATE, Optional.of("Midlertidig karakter § 9a"))
        );

        when (unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, MAX_DATE, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertEquals(1, result.size());
        assertEquals(8, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(1, result.get(RESSURS_1.navident()).errors().size());

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(DD_2Y)
            .gyldigTom(null);

        String fid_a1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_3Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a1)
            .forNavident(RESSURS_1.navident())
            .erDagligOppfolging(true)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_2Y_1D);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a1)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_2Y_1D);

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_a1)
            .tilTilknytningtype(Optional.of(Tilknytningtype.MIDLERTIDIG));

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid(fid_a2)
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_A.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_a2)
            .tilTilknytningtype(Optional.of(Tilknytningtype.MIDLERTIDIG));
    }

    @Test
    @DisplayName("Fire dvhStillinger, hvor to overlapper hverandre fullstending, men har forskjellige tilknytningstyper av type A - Ref: 11")
    void e2e_test_11() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", DD_3Y, MAX_DATE, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", DD_3Y, MAX_DATE, Optional.of("Midlertidig karakter § 9a")),
            dvhRessursStilling("A000001", "0BB0001", "Enhet B", "0BB0001", DD_3Y, MAX_DATE, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0BB0001", "Enhet B", "0BB0001", DD_3Y, MAX_DATE, Optional.of("Utdanningsstillinger § 9d"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, MAX_DATE, false, Sektor.NAV_STATLIG, Tilknytningtype.FAST),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_3Y, MAX_DATE, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertEquals(1, result.size());
        assertEquals(5, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(1, result.get(RESSURS_1.navident()).errors().size());

        String fid_a1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_3Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid(fid_a1)
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_A.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_a1)
            .tilTilknytningtype(Optional.of(Tilknytningtype.MIDLERTIDIG));

        String fid_b2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_B.getId())
            .gyldigFom(DD_3Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_b2)
            .tilTilknytningtype(Optional.of(Tilknytningtype.UTDANNINGSSTILLINGER));
    }

    @Test
    @DisplayName("Fire dvhStillinger, hvor to overlapper hverandre fullstending, men har forskjellige tilknytningstyper av type A og B - Ref: 12")
    void e2e_test_12() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", DD_3Y, DD_2Y_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0BB0001", DD_3Y, DD_2Y_1D, Optional.of("Innlån/utlån")),
            dvhRessursStilling("A000001", "0BB0001", "Enhet B", "0BB0001", DD_2Y, DD_1Y_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0BB0001", "Enhet B", "0BB0001", DD_2Y, DD_1Y_1D, Optional.of("Timelønn"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, DD_2Y_1D, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_2Y, DD_1Y_1D, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertEquals(1, result.size());
        assertEquals(0, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());

    }

    @Test
    @DisplayName("Fire dvhStillinger, en stillingperioder, hvor den lengre har tilknytning gruppa A og overlapper den kortere - Ref: 13")
    void e2e_test_13() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_3Y, DD_2Y_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_2Y, DD_1Y_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_1Y, DD_6M_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_2Y, DD_1Y_1D, Optional.of("Innlån/utlån"))
        );


        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, DD_6M_1D, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);


        assertEquals(1, result.size());
        assertEquals(1, result.get(RESSURS_1.navident()).stillingPerioder().size());
        assertEquals(0, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("Fire dvhStillinger, tre stillingperioder, hvor den kortere har tilknytning gruppa A og er midt i perioden på den lengre som ikke mappes - Ref: 14")
    void e2e_test_14() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_3Y, DD_2Y_1D, Optional.of("Innlån/utlån")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_2Y, DD_1Y_1D, Optional.of("Innlån/utlån")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_1Y, DD_6M_1D, Optional.of("Innlån/utlån")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_2Y, DD_1Y_1D, Optional.of("Fast"))
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, DD_6M_1D, true, null, null)
        ));
        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertEquals(1, result.size());
        assertEquals(3, result.get(RESSURS_1.navident()).stillingPerioder().size());
        assertEquals(8, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigTom(DD_2Y_1D);

        String fid_1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_1)
            .forNavident(A000001)
            .erDagligOppfolging(true)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_1)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_1)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        String fid_2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_1Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_2)
            .forNavident(A000001)
            .erDagligOppfolging(true)
            .gyldigFom(DD_1Y)
            .gyldigTom(DD_6M_1D);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_2)
            .gyldigFom(DD_1Y)
            .gyldigTom(DD_6M_1D);
    }

    @Test
    @DisplayName("Tre stillingperioder uten tilknytning som ikke er sammenhengende, med to stillingperioder som har tilknytning som starter og slutter midt i de periodene")
    void e2e_test_15() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_5Y, DD_4Y, Optional.of("Innlån/utlån")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_4Y_2M, DD_3Y_6M, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_3Y_8M, DD_2Y_6M, Optional.of("Innlån/utlån")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_2Y_8M, DD_1Y_6M, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_2Y, NO_END_DATE, Optional.of("Innlån/utlån"))
        );
        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_5Y, NO_END_DATE, true, null, null)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertEquals(1, result.size());
        assertEquals(5, result.get(RESSURS_1.navident()).stillingPerioder().size());
        assertEquals(14, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigTom(DD_4Y_2M.minusDays(1));

        String fid_1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_4Y_2M)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_1)
            .forNavident(A000001)
            .erDagligOppfolging(true)
            .gyldigFom(DD_4Y_2M)
            .gyldigTom(DD_3Y_6M);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_1)
            .gyldigFom(DD_4Y_2M)
            .gyldigTom(DD_3Y_6M);

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_1)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        String fid_2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_3Y_6M.plusDays(1))
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_2)
            .forNavident(A000001)
            .erDagligOppfolging(true)
            .gyldigFom(DD_3Y_6M.plusDays(1))
            .gyldigTom(DD_2Y_8M.minusDays(1));

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_2)
            .gyldigFom(DD_3Y_6M.plusDays(1))
            .gyldigTom(DD_2Y_8M.minusDays(1));

        String fid_3 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_2Y_8M)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_3)
            .forNavident(A000001)
            .erDagligOppfolging(true)
            .gyldigFom(DD_2Y_8M)
            .gyldigTom(DD_1Y_6M);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_3)
            .gyldigFom(DD_2Y_8M)
            .gyldigTom(DD_1Y_6M);

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_3)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        String fid_4 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(A000001)
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_1Y_6M.plusDays(1))
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_4)
            .forNavident(A000001)
            .erDagligOppfolging(true)
            .gyldigFom(DD_1Y_6M.plusDays(1))
            .gyldigTom(null);
    }


    // ----- Helpers

    @BeforeEach
    void fellesSetUp() {
        MockitoAnnotations.openMocks(this);
        strukturer = new NyeRessursStillingStrukturerer(
            ressursStateRepository,
            new GrupperOgSlaaSammenDvhStillinger(ressursStateRepository, unleash),
            new MatchStrukturerteStillingPerioderTilNOM(orgtilknytningStateRepository, orgenhetService),
            new SettDOLPerioderPaaStrukturerteStillinger(orgtilknytningStateRepository)
        );
        prosserer = new NyeRessursStillingProcessor(
            orgtilknytningService,
            orgtilknytningCommandService,
            ressursStateRepository,
            nomMastered,
            u4DiffPaaMastrede,
            unleash
        );
        when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(RESSURS_1));

        when(orgenhetService.exactMatchByAgressoId(ENHET_A.getAgressoId())).thenReturn(List.of(ENHET_A));
        when(orgenhetService.exactMatchByAgressoId(ENHET_B.getAgressoId())).thenReturn(List.of(ENHET_B));
        when(orgenhetService.exactMatchByAgressoId(ENHET_C.getAgressoId())).thenReturn(List.of(ENHET_C));
        when(orgenhetService.exactMatchByAgressoId(ENHET_D1.getAgressoId())).thenReturn(List.of(ENHET_D1, ENHET_D2));

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_1", "FID_2", "FID_3", "FID_4", "FID_5");
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(RESSURS_1));
    }

    static final String A000001 = "A000001";
    final RessursState RESSURS_1 = ressurs(A000001);

    final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);
    final LocalDate DD_10Y = LocalDate.now().minusYears(10).withDayOfMonth(1);   // 10 år siden
    final LocalDate DD_3Y = LocalDate.now().minusYears(3).withDayOfMonth(1);    // 3 år siden
    final LocalDate DD_3Y_1D = DD_3Y.minusDays(1);                              // 3 år siden
    final LocalDate DD_2Y = LocalDate.now().minusYears(2).withDayOfMonth(1);    // 2 år siden
    final LocalDate DD_2Y_1D = DD_2Y.minusDays(1);                              // Dagen før 2 år siden
    final LocalDate DD_1Y = LocalDate.now().minusYears(1).withDayOfMonth(1);    // 1 år siden
    final LocalDate DD_1Y_1D = DD_1Y.minusDays(1);                              // Dagen før 1 år siden
    final LocalDate DD_6M = LocalDate.now().minusMonths(6).withDayOfMonth(1); // 6 måneder siden
    final LocalDate DD_6M_1D = DD_6M.minusDays(1);                              // Dagen før 6 måneder siden

    final LocalDate DD_5Y = LocalDate.now().minusYears(5).withDayOfMonth(1); // 5 år siden
    final LocalDate DD_4Y = LocalDate.now().minusYears(4).withDayOfMonth(1); // 4 år siden
    final LocalDate DD_4Y_2M = LocalDate.now().minusYears(4).minusMonths(2).withDayOfMonth(1); // 4 år, 2 måneder siden
    final LocalDate DD_3Y_6M = LocalDate.now().minusYears(3).minusMonths(6).withDayOfMonth(1); // 3 år, 6 måneder siden
    final LocalDate DD_3Y_8M = LocalDate.now().minusYears(3).minusMonths(8).withDayOfMonth(1); // 3 år, 8 måneder siden
    final LocalDate DD_2Y_6M = LocalDate.now().minusYears(2).minusMonths(6).withDayOfMonth(1); // 2 år, 6 måneder siden
    final LocalDate DD_2Y_8M = LocalDate.now().minusYears(2).minusMonths(8).withDayOfMonth(1); // 2 år, 8 måneder siden
    final LocalDate DD_1Y_6M = LocalDate.now().minusYears(1).minusMonths(6).withDayOfMonth(1); // 1 år, 6 måneder siden
//    final LocalDate DD_3Y2M = LocalDate.now().minusYears(3).minusMonths(2).withDayOfMonth(1); // 3 år, 2 måneder siden
//    final LocalDate DD_3Y = LocalDate.now().minusYears(3).withDayOfMonth(1); // 3 år siden
//    final LocalDate DD_2Y = LocalDate.now().minusYears(2).withDayOfMonth(1); // 2 år siden
//    final LocalDate DD_2Y_1D = DD_2Y.minusDays(1);                           // Dagen før 1 år siden
//    final LocalDate DD_1Y = LocalDate.now().minusYears(1).withDayOfMonth(1); // 1 år siden
//    final LocalDate DD_1Y_1D = DD_1Y.minusDays(1);                           // Dagen før 1 år siden
//    final LocalDate DD_6M = LocalDate.now().minusMonths(6).withDayOfMonth(1); // 6 måneder siden
//    final LocalDate DD_6M_1D = DD_6M.minusDays(1);                            // Dagen før 6 måneder siden

    final LocalDate START_OM_CA_2_MND = LocalDate.now().plusMonths(3).withDayOfMonth(1);
    final LocalDate SLUTT_OM_CA_2_MND = START_OM_CA_2_MND.minusDays(1);

    IOrgenhet ENHET_A = new OrgEnhet(
        new OrgEnhetState(
            "FID_A", "a100xy", "Enhet A",
            DD_10Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0AA0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_B = new OrgEnhet(
        new OrgEnhetState(
            "FID_B", "b100xy", "Enhet B",
            DD_10Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0BB0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_C = new OrgEnhet(
        new OrgEnhetState(
            "FID_C", "c100xy", "Enhet C",
            DD_10Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0CC0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    IOrgenhet ENHET_D1= new OrgEnhet(
        new OrgEnhetState(
            "FID_D1", "d100xy", "Enhet D1",
            DD_10Y, Optional.of(DD_1Y_1D),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0DD0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    IOrgenhet ENHET_D2 = new OrgEnhet(
        new OrgEnhetState(
            "FID_D2", "d200xy", "Enhet D2",
            DD_1Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0DD0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
}