package no.nav.nom.apps.navident.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class PdlPropertiesTest {


    @Autowired PdlProperties pdlProperties;

    @MockBean private DataSource dataSource;

    @Test
    public void pdlProperties() {
        assertEquals("GEN", pdlProperties.getTema());
    }
}