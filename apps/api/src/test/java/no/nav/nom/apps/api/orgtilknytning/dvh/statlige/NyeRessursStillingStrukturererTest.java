package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.NAVIDENT_FINNES_IKKE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DisplayName("Tester gruppering og strukturering av periodisering på RessursStillinger fra DVH")
class NyeRessursStillingStrukturererTest extends RessursStillingStrukturererTestBase {
    @Autowired
    NyeRessursStillingStrukturerer CUT; // Class under test

    @Test
    @DisplayName("Validate error on navident from DVH missing in NOM")
    void test_missingNavidentInNom() {
        System.out.printf("""
                          DVH RessursStilling for Navident %s starter (%s) på eller etter ressursens sluttdato %s.
                          Behandler ikke denne eller etterfølgende DVH RessursStillinger for denne navidenten.
                          %n""", "UNKNOWN", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31));
        List<DvhRessursStilling> dvhResssursStillinger = List.of(
            dvhRessursStilling("A000001", "STILLING1", "ORGENHET1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling("UNKNOWN", "STILLING2", "ORGENHET2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        );

        when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(ressurs("A000001")));
        when(ressursStateRepository.findByNavident("UNKNOWN")).thenReturn(Optional.empty());

        var strukturerteStillingPerioder = CUT.strukturerStilingPerioderPerNavident(dvhResssursStillinger);

        assertEquals(2, strukturerteStillingPerioder.size());
        assertFalse(strukturerteStillingPerioder.get("A000001").dvhRessursStillinger().isEmpty());
        assertTrue(strukturerteStillingPerioder.get("UNKNOWN").stillingPerioder().isEmpty());
        assertEquals(1, strukturerteStillingPerioder.get("UNKNOWN").errors().size());
        assertEquals(NAVIDENT_FINNES_IKKE, strukturerteStillingPerioder.get("UNKNOWN").errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger")
    void test_GrupperPerNavident() {
        String A000001 = "A000001";
        String B000001 = "B000001";

        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = CUT.grupperDvhRessursStillingerPerNavident(dvhRessursStillinger);

        assertEquals(2, result.size());
        assertEquals(3, result.get(A000001).size());
        assertEquals(3, result.get(B000001).size());
    }


    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger - annen sortering")
    void test_GrupperPerNavident_NySortering() {
        String A000001 = "A000001";
        String B000001 = "B000001";

        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = CUT.grupperDvhRessursStillingerPerNavident(dvhRessursStillinger);

        assertEquals(2, result.size());
        assertEquals(3, result.get(A000001).size());
        assertEquals(3, result.get(B000001).size());
    }

}