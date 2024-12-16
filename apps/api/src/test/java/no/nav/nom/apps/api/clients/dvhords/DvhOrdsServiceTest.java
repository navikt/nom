package no.nav.nom.apps.api.clients.dvhords;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
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
import java.util.Optional;
import java.util.TimeZone;

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

    @Test
    void getLedere() {
        var ledere = dvhOrdsService.getLedere();
        assertNotNull(ledere);
        assertEquals(6, ledere.size());

        assertEquals("A100221", ledere.getFirst().getNavIdent());
        assertEquals("0105002", ledere.getFirst().getOrgEnhetNr());
        assertEquals("Sarpsborg Tverrfaglig 4", ledere.getFirst().getOrgenhetNavn());

        assertEquals("A100305", ledere.get(1).getNavIdent());
        assertEquals("2519003", ledere.get(1).getOrgEnhetNr());
        assertEquals("Kontaktsenter Troms og Finnmark 3", ledere.get(1).getOrgenhetNavn());
    }

    @Test
    void getOrganiseringer() {
        var organiseringer = dvhOrdsService.getOrganiseringer();
        assertNotNull(organiseringer);
        assertEquals(6, organiseringer.size());

        assertEquals("0105002", organiseringer.getFirst().getAgressoIdUnder());
        assertEquals("0105002", organiseringer.getFirst().getAgressoIdOver());
        assertEquals("Sarpsborg Tverrfaglig 4", organiseringer.getFirst().getNavnUnder());
        assertEquals("Sarpsborg Team 2", organiseringer.getFirst().getNavnOver());

        assertEquals("2519003", organiseringer.get(1).getAgressoIdUnder());
        assertEquals("251900", organiseringer.get(1).getAgressoIdOver());
        assertEquals("Kontaktsenter Troms og Finnmark 3", organiseringer.get(1).getNavnUnder());
        assertEquals("Kontaktsenter Troms og Finnmark", organiseringer.get(1).getNavnOver());
    }

    @Test
    void getOrgenheter() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo")); // Github test/build har ikke samme timezone som satt i Dockerfile
        
        var orgenheter = dvhOrdsService.getOrgenheter();
        assertNotNull(orgenheter);
        assertEquals(5, orgenheter.size());

        assertEquals("0319005", orgenheter.getFirst().getAgressoId());
        assertEquals("Søndre Nordstrand AAP", orgenheter.getFirst().getNavn());
        assertEquals("ORGENHET", orgenheter.getFirst().getOrgNiv());
        assertEquals(LocalDate.of(2015,7, 1), orgenheter.getFirst().getGyldigFom());
        assertEquals(Optional.of(LocalDate.of(2017,2,28)), orgenheter.getFirst().getGyldigTom());

        assertEquals("865200", orgenheter.get(3).getAgressoId());
        assertEquals("IT applikasjonsplattformer", orgenheter.get(3).getNavn());
        assertEquals("ORGNIV3", orgenheter.get(3).getOrgNiv());
        assertEquals(LocalDate.of(2016,10,28), orgenheter.get(3).getGyldigFom());
        assertEquals(Optional.empty(), orgenheter.get(3).getGyldigTom());
    }

    @Test
    void getTelefoner() {
        var telefoner = dvhOrdsService.getTelefoner();
        assertNotNull(telefoner);
        assertEquals(5, telefoner.size());

        assertEquals("S000001", telefoner.getFirst().getNavIdent());
        assertEquals("4740000000", telefoner.getFirst().getNummer());
        assertEquals(DvhTelefon.DvhTelefonType.TJENESTETELEFON, telefoner.getFirst().getType());
        assertEquals("Bedrift Total A (50 GB)", telefoner.getFirst().getBeskrivelse());

        assertEquals("S000002", telefoner.get(3).getNavIdent());
        assertEquals("4740000003", telefoner.get(3).getNummer());
        assertEquals(DvhTelefon.DvhTelefonType.KONTORTELEFON, telefoner.get(3).getType());
        assertEquals("Bedrift Total B (50 GB)", telefoner.get(3).getBeskrivelse());
    }
}