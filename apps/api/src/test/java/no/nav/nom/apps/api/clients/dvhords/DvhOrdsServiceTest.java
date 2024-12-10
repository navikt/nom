package no.nav.nom.apps.api.clients.dvhords;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@ContextConfiguration(classes= {DvhOrdsClientImpl.class, DvhOrdsService.class}, initializers = MockOAuth2ServerInitializer.class)
@EnableConfigurationProperties(value = AppProperties.class)
@AutoConfigureWireMock(port = 0)
class DvhOrdsServiceTest {
    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @Autowired
    DvhOrdsService dvhOrdsService;

    @Test
    void getRessurser() {
        var ressurser = dvhOrdsService.getRessurser();
        assertNotNull(ressurser);
        assertEquals(5, ressurser.size());

        assertEquals("O123456", ressurser.getFirst().getNavIdent());
        assertEquals(LocalDate.of(2008, 5, 23), ressurser.getFirst().getRemedyStartDato());
        assertTrue(ressurser.getFirst().getRemedyAktiv());

        assertEquals("O123457", ressurser.get(1).getNavIdent());
        assertEquals(LocalDate.of(2008, 5, 26), ressurser.get(1).getRemedyStartDato());
        assertNull(ressurser.get(1).getRemedySluttdato());
        assertNull(ressurser.get(1).getFysiskBrukersted());

        assertEquals("O123458", ressurser.get(2).getNavIdent());
        assertEquals(LocalDate.of(2008, 5, 28), ressurser.get(2).getRemedyStartDato());
        assertNotNull(ressurser.get(2).getRemedySluttdato());
        assertEquals("0400", ressurser.get(2).getFysiskBrukersted());
        assertEquals("N", ressurser.get(2).getAnsattRessursKode());
        assertFalse(ressurser.get(2).getRemedyAktiv());
    }
}