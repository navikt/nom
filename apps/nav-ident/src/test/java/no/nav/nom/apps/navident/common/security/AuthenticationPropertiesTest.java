package no.nav.nom.apps.navident.common.security;

import no.nav.nom.apps.navident.SpringBootTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationPropertiesTest extends SpringBootTestBase {
    @Autowired
    AuthenticationProperties authProperties;


    @Test
    public void authProperties() {
        assertEquals("test", authProperties.getAad().getClientId());
    }
}