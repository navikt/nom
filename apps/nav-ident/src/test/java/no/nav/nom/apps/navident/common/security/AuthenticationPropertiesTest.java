package no.nav.nom.apps.navident.common.security;

import no.nav.nom.apps.navident.common.PdlProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
class AuthenticationPropertiesTest {


    @Autowired
    AuthenticationProperties authProperties;

    @MockBean private DataSource dataSource;

    @Test
    public void authProperties() {
        assertEquals("test", authProperties.getAad().getClientId());
    }
}