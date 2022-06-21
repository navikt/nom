package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.*;
import no.nav.nom.apps.navident.pdl.PdlService;
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
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RnRNeedTest {

    @Autowired private Map<String, Navident> navidenter;
    @Autowired private NeedProducer needProducer;
    @MockBean private DataSource dataSource;
    @Value("${test.rnr-topic}")  private String topic;
    @Autowired private RnRTestListener rnrTestListener;
    @Autowired private RnRTestNeedProducer rnrTestNeedProducer;

    @MockBean private PdlService pdlService;

    @BeforeEach
    public void emptyNeedsRead() { rnrTestListener.needsRead.clear(); }

    @BeforeEach
    public void resetNavidenterDomainData() { prepNavidenter(); }

    @Test
    public void createNeed() {
        String id = "01019912345";
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).pollDelay(200, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2, populatedNeed.personid);
        assertEquals(TIDLIGERE_ANSATT_FLERE_ID_3_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_NavIdFinnesIkke() {
        String id =  "01019912345";
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.personOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited();

        assertEquals(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId, populatedNeed.personid);
        assertEquals(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2, populatedNeed.eksisterendeId); // Dette er den med nyeste dato
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.pdlOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.pdlOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.pdlOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.pdlOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.pdlOppslagVisited() );
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.idGeneratorVisited() );
        Need populatedNeed = rnrTestListener.getNeed_idGeneratorVisited();

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

        Awaitility.await().atMost(1, SECONDS).pollDelay(200, MILLISECONDS).pollInterval(10, MILLISECONDS).until( () -> !rnrTestListener.idGeneratorVisited() );

        // After 200 ms (poll delay), expect no id generated
        assertFalse( rnrTestListener.needsRead.stream().anyMatch( n -> n.isIdGeneratorVisited() ) );
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.responsProdusentVisited() );
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited();

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(201, respons.code);
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.responsProdusentVisited() );
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited();

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(200, respons.code);
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.responsProdusentVisited() );
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited();

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(201, respons.code);
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.responsProdusentVisited() );
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited();

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(201, respons.code);
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.responsProdusentVisited() );
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited();

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(200, respons.code);
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

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until( () -> rnrTestListener.responsProdusentVisited() );
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited();

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(404, respons.code);
        assertEquals("C000001", respons.body.navident);
        assertEquals(LocalDate.now(), respons.body.aktivFra);
        assertNull(respons.body.aktivTil);
        assertNotNull(respons.body.feilmelding);
    }

    private void prepNavidenter() {
        navidenter.put(ROBOT_1_NavId, robot_1());
        navidenter.put(ROBOT_2_NavId, robot_2());
        navidenter.put(TIDLIGERE_ANSATT_1_NavId, tidligere_ansatt_1());
        navidenter.put(NÅVÆRENDE_ANSATT_2_NavId, nåværende_ansatt_2());
        navidenter.put(TIDLIGERE_ANSATT_FLERE_ID_3_NavId, tidligere_ansatt_flere_id_3());

        navidenter.put(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_1, tidligere_ansatt_duplikat_navId_1());
        navidenter.put(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2, tidligere_ansatt_duplikat_navId_2());

    }

    private static String ROBOT_1_PersonId = "X1019900001";
    private static String ROBOT_1_NavId = "X000001";

    private static String ROBOT_2_PersonId = "X1019900002";
    private static String ROBOT_2_NavId = "X000002";

    private static String TIDLIGERE_ANSATT_1_PersonId = "01019900001";
    private static String TIDLIGERE_ANSATT_1_NavId = "A000001";

    private static String NÅVÆRENDE_ANSATT_2_PersonId = "01019900002";
    private static String NÅVÆRENDE_ANSATT_2_NavId = "A000002";

    private static String TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1= "01019900003";
    private static String TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2= "02019900003";
    private static String TIDLIGERE_ANSATT_FLERE_ID_3_NavId = "A000003";

    private static String TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId = "01019900004";
    private static String TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_1 = "A000004";
    private static String TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2 = "B000004";



    private Navident robot_1() {
        return Navident.builder()
                .navident(ROBOT_1_NavId)
                .identType(IdentType.ROBOT)
                .gjeldendePersonid(ROBOT_1_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(ROBOT_1_PersonId).type(PersonidType.MANUELL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).build()
                ))
                .build();
    }


    private Navident robot_2() {
        return Navident.builder()
                .navident(ROBOT_2_NavId)
                .identType(IdentType.ROBOT)
                .gjeldendePersonid(ROBOT_2_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(ROBOT_2_PersonId).type(PersonidType.MANUELL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusMonths(6)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_1() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_1_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_1_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_1_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusYears(1)).build()
                ))
                .build();
    }

    private Navident nåværende_ansatt_2() {
        return Navident.builder()
                .navident(NÅVÆRENDE_ANSATT_2_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(NÅVÆRENDE_ANSATT_2_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(NÅVÆRENDE_ANSATT_2_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_flere_id_3() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_FLERE_ID_3_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1).type(PersonidType.PDL).build(),
                        Personid.builder().id(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusYears(1)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_duplikat_navId_1() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_1)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusYears(1)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_duplikat_navId_2() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusMonths(8)).tilOgMed(LocalDate.now().minusMonths(2)).build()
                ))
                .build();
    }

}