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
import java.util.List;
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
    void getKommunaleRessurser() {
        var ressurser = dvhOrdsService.getKommunaleRessurser(List.of("A321001", "A321002", "A321004", "A321005"));
        assertNotNull(ressurser);
        assertEquals(4, ressurser.size());

        assertEquals("A321001", ressurser.getFirst().getNavIdent());
        assertEquals(LocalDate.of(2013, 1, 9), ressurser.getFirst().getRemedyStartDato());
        assertEquals(LocalDate.of(2020, 1, 2), ressurser.getFirst().getRemedySluttdato());
        assertFalse(ressurser.getFirst().getRemedyAktiv());
        assertEquals("080600", ressurser.getFirst().getOrgenhetPaaRessurs());
        assertEquals("Skien", ressurser.getFirst().getOrgenhetPaaRessursBeskrivelse());
        assertEquals("ORGNIV3", ressurser.getFirst().getOrgenhetPaaRessursNiva());

        assertEquals("A321002", ressurser.get(1).getNavIdent());
        assertEquals(LocalDate.of(2024, 5, 8), ressurser.get(1).getRemedyStartDato());
        assertNull(ressurser.get(1).getSluttdato());
        assertEquals("0906", ressurser.get(1).getFysiskBrukersted());
        assertTrue(ressurser.get(1).getRemedyAktiv());
        assertEquals("0906003", ressurser.get(1).getOrgenhetPaaRessurs());
        assertEquals("Arendal Ungdom", ressurser.get(1).getOrgenhetPaaRessursBeskrivelse());
        assertEquals("ORGENHET", ressurser.get(1).getOrgenhetPaaRessursNiva());

        assertEquals("A321005", ressurser.get(3).getNavIdent());
        assertEquals(LocalDate.of(2009, 8, 4), ressurser.get(3).getRemedyStartDato());
        assertNotNull(ressurser.get(3).getRemedySluttdato());
        assertEquals("0326", ressurser.get(3).getFysiskBrukersted());
        assertEquals("N", ressurser.get(3).getAnsattRessursKode());
        assertTrue(ressurser.get(3).getRemedyAktiv());
        assertEquals("0326006", ressurser.get(3).getOrgenhetPaaRessurs());
        assertEquals("Alna Inngang", ressurser.get(3).getOrgenhetPaaRessursBeskrivelse());
        assertEquals("ORGENHET", ressurser.get(3).getOrgenhetPaaRessursNiva());
    }

    @Test
    void getEksterneRessurser() {
        var ressurser = dvhOrdsService.getEksterneTilknytninger();
        assertNotNull(ressurser);
        assertEquals(5, ressurser.size());

        assertEquals("M000001", ressurser.getFirst().navident());
        assertEquals(LocalDate.of(2013, 4, 2), ressurser.getFirst().remedyStart());
        assertEquals(LocalDate.of(2019, 1, 29), ressurser.getFirst().remedySlutt());
        assertFalse(ressurser.getFirst().remedyAktiv());
        assertEquals("190200", ressurser.getFirst().orgenhetPaaRessurs());
        assertEquals("ORGNIV3", ressurser.getFirst().orgenhetPaaRessursNivaa());
        assertEquals("1902", ressurser.getFirst().remedyEnhetId());

        assertEquals("S000001", ressurser.get(1).navident());
        assertEquals(LocalDate.of(2017, 2, 20), ressurser.get(1).remedyStart());
        assertEquals(LocalDate.of(2019, 1, 29), ressurser.get(1).remedySlutt());
        assertFalse(ressurser.get(1).remedyAktiv());
        assertEquals("190200", ressurser.get(1).orgenhetPaaRessurs());
        assertEquals("ORGNIV3", ressurser.get(1).orgenhetPaaRessursNivaa());
        assertEquals("1902", ressurser.get(1).remedyEnhetId());

        assertEquals("G000001", ressurser.get(4).navident());
        assertEquals(LocalDate.of(2012, 11, 14), ressurser.get(4).remedyStart());
        assertEquals(LocalDate.of(2019, 1, 29), ressurser.get(4).remedySlutt());
        assertFalse(ressurser.get(4).remedyAktiv());
        assertEquals("855210", ressurser.get(4).orgenhetPaaRessurs());
        assertEquals("ORGNIV3", ressurser.get(4).orgenhetPaaRessursNivaa());
        assertEquals("2990", ressurser.get(4).remedyEnhetId());
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

    @Test
    void getRessursStillinger() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo")); // Github test/build har ikke samme timezone som satt i Dockerfile

        var ressursStillinger = dvhOrdsService.getStatligeTilknytninger();
        assertNotNull(ressursStillinger);
        assertEquals(5, ressursStillinger.size());

        assertEquals("A100036", ressursStillinger.getFirst().getNavident().get());
        assertEquals("0499000", ressursStillinger.getFirst().getStillingAgressoId().get());
        assertEquals("Innlandet Arbeidslivssenter", ressursStillinger.getFirst().getStillingOrgenhetNavn());
        assertEquals(LocalDate.of(2021,7,1), ressursStillinger.getFirst().getStillingDatoFom());
        assertEquals(LocalDate.of(2021,12,31), ressursStillinger.getFirst().getStillingDatoTom());
        assertEquals(LocalDate.of(2006,4,4), ressursStillinger.getFirst().getRessursDvhDatoFom());
        assertEquals(LocalDate.of(2099,12,31), ressursStillinger.getFirst().getRessursDvhDatoTom());

        assertEquals("A100180", ressursStillinger.get(4).getNavident().get());
        assertEquals("0521000", ressursStillinger.get(4).getStillingAgressoId().get());
        assertEquals("Øyer", ressursStillinger.get(4).getStillingOrgenhetNavn());
        assertEquals(LocalDate.of(2024,6,1), ressursStillinger.get(4).getStillingDatoFom());
        assertEquals(LocalDate.of(2024,6,30), ressursStillinger.get(4).getStillingDatoTom());
        assertEquals(LocalDate.of(1980,5,19), ressursStillinger.get(4).getRessursDvhDatoFom());
        assertEquals(LocalDate.of(2099,12,31), ressursStillinger.get(4).getRessursDvhDatoTom());
    }
}