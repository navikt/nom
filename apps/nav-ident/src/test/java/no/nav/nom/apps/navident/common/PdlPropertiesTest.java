package no.nav.nom.apps.navident.common;

import no.nav.nom.apps.navident.SpringBootTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdlPropertiesTest extends SpringBootTestBase {
    @Autowired
    PdlConfig pdlProperties;

    @Test
    public void pdlProperties() {
        assertEquals("GEN", pdlProperties.getTema());
    }
}