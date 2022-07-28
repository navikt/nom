package no.nav.nom.apps.navident.pdl;

import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.apps.navident.pdl.PdlConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdlConfigTest extends SpringBootTestBase {
    @Autowired
    PdlConfig pdlProperties;

    @Test
    public void pdlProperties() {
        assertEquals("GEN", pdlProperties.getTema());
    }
}