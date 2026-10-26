package no.nav.nom.apps.api.rest.uttrekk;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OmadaUttrekkTest {

    private final OmadaUttrekk omadaUttrekk;

    OmadaUttrekkTest() {
        var orgenhet = new AppProperties.Orgenhet(null, null, null, null, null, null, null, null, null);
        var appProperties = new AppProperties(null, null, null, null, null, orgenhet);
        omadaUttrekk = new OmadaUttrekk(
            null, null,
            null, null, null,
            null, null,
            null, appProperties,
            null, mock(NomStateOrganiseringListener.class),
            mock(NomStateOrgtilknytningListener.class),
            mock(NomStateOrgEnhetListener.class)
        );
    }


    @ParameterizedTest(name = "Input: {0} Output: {1}")
    @DisplayName("Should format privattelefon to standard format")
    @MethodSource("privattelefon_varianter_input")
    void should_format_privattelefon_to_standard_format(String input, String output) {

        String formatertTlf = omadaUttrekk.formaterTelefonnummer(input);
        assertThat(formatertTlf).isEqualTo(output);
    }

    private static Stream<Arguments> privattelefon_varianter_input() {
        return Stream.of(
            Arguments.of("12345678", "+4712345678"),
            Arguments.of("12 34 56 78", "+4712345678"),
            Arguments.of("123 45 678", "+4712345678"),
            Arguments.of("+3512345678", "+3512345678"),
            Arguments.of("+4712345678", "+4712345678"),
            Arguments.of("4712345678", "+4712345678"),
            Arguments.of("47345678", "+4747345678")
        );
    }
}