package no.nav.nom.apps.testdata.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RekkefolgeValidatorTest {
    private static final String FEILMELDING = "Rekkefølgen må inneholde alle rad-ID-ene nøyaktig én gang";

    @Test
    void bevarerRekkefolgeOgReturnererUavhengigKopiUtenAaEndreInput() {
        var ids = new ArrayList<>(List.of(3L, 1L, 2L));

        var resultat = RekkefolgeValidator.kontrollerRekkefolge(
            ids, List.of(new SyntheticRow(1), new SyntheticRow(2), new SyntheticRow(3)), SyntheticRow::id
        );

        assertEquals(List.of(3L, 1L, 2L), resultat);
        assertEquals(List.of(3L, 1L, 2L), ids);
        assertNotSame(ids, resultat);
        ids.set(0, 9L);
        assertEquals(List.of(3L, 1L, 2L), resultat);
    }

    @Test
    void godtarTomRekkefolgeForTomtUtvalg() {
        assertEquals(List.of(), RekkefolgeValidator.kontrollerRekkefolge(List.of(), List.of(), SyntheticRow::id));
    }

    @Test
    void avviserNullRekkefolgeMedEksaktFeilmelding() {
        assertFeil(null, List.of(new SyntheticRow(1)));
    }

    @Test
    void avviserNullIdMedEksaktFeilmelding() {
        assertFeil(java.util.Arrays.asList(1L, null), List.of(new SyntheticRow(1)));
    }

    @Test
    void avviserDuplikaterMedEksaktFeilmelding() {
        assertFeil(List.of(1L, 1L), List.of(new SyntheticRow(1)));
    }

    @Test
    void avviserManglendeIdMedEksaktFeilmelding() {
        assertFeil(List.of(1L), List.of(new SyntheticRow(1), new SyntheticRow(2)));
    }

    @Test
    void avviserFremmedIdMedEksaktFeilmelding() {
        assertFeil(List.of(1L, 3L), List.of(new SyntheticRow(1), new SyntheticRow(2)));
    }

    @Test
    void brukerIdExtractorForAnnenRadtype() {
        var resultat = RekkefolgeValidator.kontrollerRekkefolge(
            List.of(20L, 10L), List.of(new AnnenRad(10), new AnnenRad(20)), AnnenRad::radId
        );

        assertEquals(List.of(20L, 10L), resultat);
    }

    private static void assertFeil(List<Long> ids, List<SyntheticRow> rows) {
        var feil = assertThrows(
            IllegalArgumentException.class,
            () -> RekkefolgeValidator.kontrollerRekkefolge(ids, rows, SyntheticRow::id)
        );
        assertEquals(FEILMELDING, feil.getMessage());
    }

    private record SyntheticRow(long id) {
    }

    private record AnnenRad(long radId) {
    }
}
