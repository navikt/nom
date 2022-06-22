package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.idgenerator.NavidGenerator;
import no.nav.nom.apps.navident.pdl.PdlService;
import no.nav.nom.apps.navident.rnrservices.util.DomainTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdentServiceImplTest {

    @Value("${test.rnr-topic}")  private String topic;

    @MockBean  private DataSource dataSource;
    @MockBean private PdlService pdlService;
    @MockBean private NavidGenerator navidGenerator;
    @Autowired private DomainTestData domainTestData;

    @Autowired private Map<String, Navident> navidenter;
    @Autowired private IdentService identService;

    @BeforeEach
    public void resetNavidenterDomainData() { domainTestData.prepNavidenter(navidenter); }

    @Test
    public void bestillIdent_nyansatt() {
        String personid = IKKE_TIDLIGERE_ANSATT_1_PersonId;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Arrays.asList(personid));
        when(navidGenerator.generate(prefix)).thenReturn("A100001");

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("A100001", respons.body.getNavident());
        assertEquals(LocalDate.now(), respons.body.getAktivFra());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_tidligereAnsatt() {
        String personid = TIDLIGERE_ANSATT_1_PersonId;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Arrays.asList(personid));

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(TIDLIGERE_ANSATT_1_NavId, respons.body.getNavident());
        assertEquals(LocalDate.now(), respons.body.getAktivFra());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_tidligereAnsatt_FlerePersonideer_1() {
        String personid = TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Arrays.asList(personid));

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_NavId, respons.body.getNavident());
        assertEquals(LocalDate.now(), respons.body.getAktivFra());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_tidligereAnsatt_FlerePersonideer_2() {
        String personid = TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Arrays.asList(personid));

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_NavId, respons.body.getNavident());
        assertEquals(LocalDate.now(), respons.body.getAktivFra());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_nåværendeAnsatt() {
        String personid = NÅVÆRENDE_ANSATT_2_PersonId;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Arrays.asList(personid));

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.CONFLICT, respons.httpStatus);
        assertEquals(NÅVÆRENDE_ANSATT_2_NavId, respons.body.getNavident());
        assertNotEquals(LocalDate.now(), respons.body.getAktivFra());
        assertNotNull(respons.body.getFeilmelding());
    }
}