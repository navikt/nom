package no.nav.nom.apps.nom.unit4.unit4;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void convertName() {
        Assertions.assertThat(Utils.convertName("ELVIS")).isEqualTo("Elvis");
        Assertions.assertThat(Utils.convertName("ELVIS-AARON")).isEqualTo("Elvis-Aaron");
        Assertions.assertThat(Utils.convertName("ELVIS-AARON PRESLEY")).isEqualTo("Elvis-Aaron Presley");
    }

}