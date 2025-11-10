package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;
import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {

    @Test
    @DisplayName("Ensure that Utils.fifteenCharLengthZeroPaddedNumber works as expected")
    void testFifteenCharLengthZeroPaddedNumber(){

        assertThat(fifteenCharLengthZeroPaddedNumber(23L)).isEqualTo("000000000000023");
    }

    @Test
    @DisplayName("Ensure that Utils.capitalizeNavn works as expected")
    void testCapitalizeNavn(){

        assertThat(capitalizeNavn("fornavn")).isEqualTo("Fornavn");
        assertThat(capitalizeNavn("flere fornavn")).isEqualTo("Flere Fornavn");
    }

    @Test
    @DisplayName("Ensure that Utils.epochSeconds works as expected")
    void testEpochSeconds(){

        assertThat(epochSecondsNorwegianTimeNoon(LocalDate.of(2023, 12, 12)))
            .isEqualTo(1702378800L);
    }

    @Test
    @DisplayName("Ensure that Utils.formatWithMellomNavn works as expected")
    void testFormatWithMellomNavn(){
        var navn = new no.nav.nom.contract.nom.eventsource.Visningsnavn("fornavn", Optional.empty(), "etternavn");
        assertThat(formatFornavnWithMellomnavn(navn)).isEqualTo("Fornavn");

        navn = new no.nav.nom.contract.nom.eventsource.Visningsnavn("fornavn", Optional.of("van der"), "etternavn");
        assertThat(formatFornavnWithMellomnavn(navn)).isEqualTo("Fornavn van der");

        navn = new no.nav.nom.contract.nom.eventsource.Visningsnavn("fornavn", Optional.of(""), "etternavn");
        assertThat(capitalizeNavn(formatFornavnWithMellomnavn(navn))).isEqualTo("Fornavn");

        navn = new no.nav.nom.contract.nom.eventsource.Visningsnavn("fornavn", Optional.of("    "), "etternavn");
        assertThat(capitalizeNavn(formatFornavnWithMellomnavn(navn))).isEqualTo("Fornavn");
    }
}
