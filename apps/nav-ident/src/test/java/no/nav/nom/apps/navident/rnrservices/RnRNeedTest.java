package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.pdl.PdlService;
import no.nav.nom.apps.navident.rnrservices.util.DomainTestData;
import no.nav.nom.apps.navident.rnrservices.util.RnRTestListener;
import no.nav.nom.apps.navident.rnrservices.util.RnRTestNeedProducer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.*;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RnRNeedTest {

    @Autowired private Map<String, Navident> navidenter;

    @Autowired    private NeedProducer needProducer;
    @MockBean    private DataSource dataSource;
    @Value("${test.rnr-topic}")    private String topic;
    @Autowired    private RnRTestListener rnrTestListener;
    @Autowired    private RnRTestNeedProducer rnrTestNeedProducer;
    @Autowired private DomainTestData domainTestData;

    @MockBean    private PdlService pdlService;

    @BeforeEach
    public void emptyNeedsRead() {
        rnrTestListener.needsRead.clear();
    }

    @BeforeEach
    public void resetNavidenterDomainData() {
        domainTestData.prepNavidenter(navidenter);
    }

    @Test
    public void createNeed() {
        String id = "51019955555";
        needProducer.produceNeed('A', id, IdentType.STANDARD);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.needsRead.size() >= 1);

        List<Need> needs = rnrTestListener.needsRead.stream().filter(n -> n.visited == 0).collect(Collectors.toList());
        assertEquals(1, needs.size());
        assertEquals(id, needs.get(0).personid);
        assertEquals('A', needs.get(0).idPrefix);
        assertEquals(IdentType.STANDARD, needs.get(0).identType);
        assertEquals(LocalDate.now(), needs.get(0).fraDato);
    }


    @Test
    public void personOppslagId_NeedStandard_FinnesIkkeIPDL() {
        String id = "01019912345";
        Need need = Need.builder()
                .idPrefix('A')
                .personid(id)
                .pdlOppslagVisited(true)
                .pdlPersonider(Collections.emptyList())
                .identType(IdentType.STANDARD)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);


        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.needsRead.size() >= 1);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).pollDelay(200, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited("01019912345"));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited("01019912345");

        assertEquals(id, populatedNeed.personid);
        assertNull(populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_NeedRobot_FinnesIkke() {
        String id = "X1019912345";
        Need need = Need.builder()
                .idPrefix('X')
                .personid(id)
                .identType(IdentType.ROBOT)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited("X1019912345"));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited("X1019912345");

        assertEquals(id, populatedNeed.personid);
        assertNull(populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_NeedRobot_Finnes() {
        Need need = Need.builder()
                .idPrefix('X')
                .personid(ROBOT_1_PersonId)
                .identType(IdentType.ROBOT)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(ROBOT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(ROBOT_1_PersonId);

        assertEquals(ROBOT_1_PersonId, populatedNeed.personid);
        assertEquals(ROBOT_1_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_TidligereAnsatt() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(TIDLIGERE_ANSATT_1_PersonId)
                .pdlPersonider(Arrays.asList(TIDLIGERE_ANSATT_1_PersonId))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(TIDLIGERE_ANSATT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(TIDLIGERE_ANSATT_1_PersonId);

        assertEquals(TIDLIGERE_ANSATT_1_PersonId, populatedNeed.personid);
        assertEquals(TIDLIGERE_ANSATT_1_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_NåværendeAnsatt() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(NÅVÆRENDE_ANSATT_2_PersonId)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(NÅVÆRENDE_ANSATT_2_PersonId)
                .pdlPersonider(Arrays.asList(NÅVÆRENDE_ANSATT_2_PersonId))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(NÅVÆRENDE_ANSATT_2_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(NÅVÆRENDE_ANSATT_2_PersonId);

        assertEquals(NÅVÆRENDE_ANSATT_2_PersonId, populatedNeed.personid);
        assertEquals(NÅVÆRENDE_ANSATT_2_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_TidligereAnsatt_Flere_IDer_1() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1)
                .pdlPersonider(Arrays.asList(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1);

        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, populatedNeed.personid);
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_TidligereAnsatt_Flere_IDer_2() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1)
                .pdlPersonider(Arrays.asList(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2);

        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2, populatedNeed.personid);
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_NavIdFinnesIkke() {
        String id = "01019912345";
        Need need = Need.builder()
                .idPrefix('A')
                .personid(id)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(id)
                .pdlPersonider(Arrays.asList(id))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited("01019912345"));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited("01019912345");

        assertEquals(id, populatedNeed.personid);
        assertNull(populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_TidligereAnsatt_Duplikat_NavIDer() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId)
                .pdlPersonider(Arrays.asList(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId))
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId);

        assertEquals(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId, populatedNeed.personid);
        assertEquals(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2, populatedNeed.eksisterendeId); // Dette er den med nyeste dato
    }

    @Test
    public void personOppslagId_Need_PdlIkkeFunnet_TidligereAnsatt() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(null)
                .pdlPersonider(Collections.emptyList())
                .idGeneratorVisited(true)
                .generertId(null)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(TIDLIGERE_ANSATT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(TIDLIGERE_ANSATT_1_PersonId);

        assertEquals(TIDLIGERE_ANSATT_1_PersonId, populatedNeed.personid);
        assertEquals(TIDLIGERE_ANSATT_1_NavId, populatedNeed.eksisterendeId); // Selv om PDL feiler, bruk id som finnes
    }



    @Test
    public void pdlOppslag_Need_IdIkkeFunnet_TomRespons() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(TIDLIGERE_ANSATT_1_PersonId)).thenReturn(Arrays.asList());

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(TIDLIGERE_ANSATT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(TIDLIGERE_ANSATT_1_PersonId);

        assertNull(populatedNeed.pdlGjeldendePersonid);
        assertTrue(populatedNeed.pdlPersonider.isEmpty());
    }

    @Test
    public void pdlOppslag_Need_IdIkkeFunnet_NullRespons() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(TIDLIGERE_ANSATT_1_PersonId)).thenReturn(null);

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(TIDLIGERE_ANSATT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(TIDLIGERE_ANSATT_1_PersonId);

        assertNull(populatedNeed.pdlGjeldendePersonid);
        assertTrue(populatedNeed.pdlPersonider.isEmpty());
    }

    @Test
    public void pdlOppslag_Need_IdFunnet_1_ident() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(TIDLIGERE_ANSATT_1_PersonId)).thenReturn(Arrays.asList(TIDLIGERE_ANSATT_1_PersonId));

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(TIDLIGERE_ANSATT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(TIDLIGERE_ANSATT_1_PersonId);

        assertEquals(TIDLIGERE_ANSATT_1_PersonId, populatedNeed.pdlGjeldendePersonid);
        assertEquals(1, populatedNeed.pdlPersonider.size());
        assertEquals(TIDLIGERE_ANSATT_1_PersonId, populatedNeed.pdlPersonider.get(0));
    }

    @Test
    public void pdlOppslag_Need_IdFunnet_2_identer() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1)
                .identType(IdentType.STANDARD)
                .idGeneratorVisited(true)
                .personOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1)).thenReturn(Arrays.asList(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2));

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1);

        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, populatedNeed.pdlGjeldendePersonid);
        assertEquals(2, populatedNeed.pdlPersonider.size());
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, populatedNeed.pdlPersonider.get(0));
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2, populatedNeed.pdlPersonider.get(1));
    }

    @Test
    public void pdlOppslag_Need_IdFunnet_2_identer_v2() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2)).thenReturn(Arrays.asList(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2));

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2);

        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, populatedNeed.pdlGjeldendePersonid);
        assertEquals(2, populatedNeed.pdlPersonider.size());
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1, populatedNeed.pdlPersonider.get(0));
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2, populatedNeed.pdlPersonider.get(1));
    }

    @Test
    public void navIdGenerator_personOppslagVisited_ikkeFunnet() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();


        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.idGeneratorVisited(TIDLIGERE_ANSATT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_idGeneratorVisited(TIDLIGERE_ANSATT_1_PersonId);

        assertNotNull(populatedNeed.generertId);
        assertTrue(populatedNeed.generertId.startsWith("A"));
    }

    @Test
    public void navIdGenerator_personOppslagVisited_Funnet() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .eksisterendeId(TIDLIGERE_ANSATT_1_NavId)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();


        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollDelay(200, MILLISECONDS).pollInterval(10, MILLISECONDS).until(() -> !rnrTestListener.idGeneratorVisited(TIDLIGERE_ANSATT_1_PersonId));

        // After 200 ms (poll delay), expect no id generated
        assertFalse(rnrTestListener.needsRead.stream().anyMatch(n -> n.isIdGeneratorVisited() && n.getPersonid().equals(TIDLIGERE_ANSATT_1_PersonId)));
    }

    @Test
    public void navIdGenerator_PdlIkkeFunnet_personOppslagIkkeFunnet() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid("01019922222")
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(null)
                .pdlPersonider(Collections.emptyList())
                .personOppslagVisited(true)
                .eksisterendeId(null)
                .responsProdusentVisited(true)
                .build();

        rnrTestNeedProducer.publish(need);


        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.idGeneratorVisited("01019922222"));
        Need populatedNeed = rnrTestListener.getNeed_idGeneratorVisited("01019922222");

        assertEquals("01019922222", populatedNeed.personid);
        assertNull(populatedNeed.eksisterendeId);
        assertNotNull(populatedNeed.generertId);
    }

    @Test
    public void responsProdusent_Robot_NyIdent() {
        Need need = Need.builder()
                .personid("Y1019900002")
                .identType(IdentType.ROBOT)
                .fraDato(LocalDate.now())
                .personOppslagVisited(true)
                .eksisterendeId(null)
                .idGeneratorVisited(true)
                .generertId("Y000002")
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited("Y1019900002"));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited("Y1019900002");

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("Y000002", respons.body.navident);
        assertEquals(LocalDate.now(), respons.body.aktivFra);
        assertNull(respons.body.aktivTil);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Robot_Eksisterende_Ikkeaktiv() {
        Need need = Need.builder()
                .personid("X1019900002")
                .identType(IdentType.ROBOT)
                .fraDato(LocalDate.now())
                .personOppslagVisited(true)
                .eksisterendeId("X000002")
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited("X1019900002"));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited("X1019900002");

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals("X000002", respons.body.navident);
        assertEquals(LocalDate.now(), respons.body.aktivFra);
        assertNull(respons.body.aktivTil);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Standard_IkkeEksisterende() {
        Need need = Need.builder()
                .personid("01019912345")
                .identType(IdentType.STANDARD)
                .fraDato(LocalDate.now())
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .pdlPersonider(Arrays.asList("01019912345"))
                .pdlGjeldendePersonid("01019912345")
                .idGeneratorVisited(true)
                .generertId("C000001")
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited("01019912345"));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited("01019912345");

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("C000001", respons.body.navident);
        assertEquals(LocalDate.now(), respons.body.aktivFra);
        assertNull(respons.body.aktivTil);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Standard_IkkeEksisterende_PdlFunnetMenIkkeGjeldende() {
        Need need = Need.builder()
                .personid("01019912345")
                .identType(IdentType.STANDARD)
                .fraDato(LocalDate.now())
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .pdlPersonider(Arrays.asList("01019912345", "11019912345"))
                .pdlGjeldendePersonid("11019912345")
                .idGeneratorVisited(true)
                .generertId("C000001")
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited("01019912345"));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited("01019912345");

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("C000001", respons.body.navident);
        assertEquals(LocalDate.now(), respons.body.aktivFra);
        assertNull(respons.body.aktivTil);
        assertNull(respons.body.feilmelding);
    }


    @Test
    public void responsProdusent_Standard_Eksisterende_Ikkeaktiv() {
        Need need = Need.builder()
                .personid(TIDLIGERE_ANSATT_1_PersonId)
                .identType(IdentType.STANDARD)
                .fraDato(LocalDate.now())
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(TIDLIGERE_ANSATT_1_PersonId)
                .pdlPersonider(Arrays.asList(TIDLIGERE_ANSATT_1_PersonId))
                .eksisterendeId(TIDLIGERE_ANSATT_1_NavId)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited(TIDLIGERE_ANSATT_1_PersonId));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited(TIDLIGERE_ANSATT_1_PersonId);

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(TIDLIGERE_ANSATT_1_NavId, respons.body.navident);
        assertEquals(LocalDate.now(), respons.body.aktivFra);
        assertNull(respons.body.aktivTil);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Standard_IkkeEksisterende_PdlIkkeFunnet() {
        Need need = Need.builder()
                .personid("01010100000")
                .identType(IdentType.STANDARD)
                .fraDato(LocalDate.now())
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .idGeneratorVisited(true)
                .generertId("C000001")
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited("01010100000"));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited("01010100000");

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.NOT_FOUND, respons.httpStatus);
        assertEquals("C000001",respons.body.navident);
        assertEquals(LocalDate.now(), respons.body.aktivFra);
        assertNull(respons.body.aktivTil);
        assertNotNull(respons.body.feilmelding);
    }

}