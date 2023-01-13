package no.nav.nom.apps.nom.unit4.unit4;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void convertName() {
        Assertions.assertThat(Utils.formatName("ELVIS")).isEqualTo("Elvis");
        Assertions.assertThat(Utils.formatName("ELVIS-AARON")).isEqualTo("Elvis-Aaron");
        Assertions.assertThat(Utils.formatName("ELVIS-AARON PRESLEY")).isEqualTo("Elvis-Aaron Presley");

        Assertions.assertThat(Utils.formatName("elvis-aaron presley")).isEqualTo("Elvis-Aaron Presley");
        Assertions.assertThat(Utils.formatName("Elvis-Aaron Presley")).isEqualTo("Elvis-Aaron Presley");
    }

}