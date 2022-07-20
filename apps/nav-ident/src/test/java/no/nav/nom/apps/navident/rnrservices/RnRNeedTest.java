package no.nav.nom.apps.navident.rnrservices;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.EmbeddedKafkaTestBase;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.eventsource.EventSourceService;
import no.nav.nom.apps.navident.idgenerator.NavidRepository;
import no.nav.nom.apps.navident.pdl.PdlService;
import no.nav.nom.apps.navident.rnrservices.util.DomainTestData;
import no.nav.nom.apps.navident.rnrservices.util.RnRTestListener;
import no.nav.nom.apps.navident.rnrservices.util.RnRTestNeedProducer;
import no.nav.nom.libs.test.RsaStub;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class RnRNeedTest extends EmbeddedKafkaTestBase {

    @Autowired private Map<String, Navident> navidenter;

    @Autowired    private NeedProducer needProducer;
    @Value("${test.rnr-topic}")    private String topic;
    @Autowired    private RnRTestListener rnrTestListener;
    @Autowired    private RnRTestNeedProducer rnrTestNeedProducer;
    @Autowired private DomainTestData domainTestData;

    @MockBean private PdlService pdlService;
    @MockBean private IdentService identService;
    @MockBean private EventSourceService eventSourceService;

    @MockBean private NavidRepository navidRepository;

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
        String uuid = needProducer.produceNeed('A', id, IdentType.STANDARD);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.needsRead.size() >= 1);

        List<Need> needs = rnrTestListener.needsRead.stream().filter(n -> n.visited == 0).collect(Collectors.toList());
        assertEquals(1, needs.size());
        assertEquals(id, needs.get(0).personid);
        assertEquals('A', needs.get(0).idPrefix);
        assertEquals(IdentType.STANDARD, needs.get(0).identType);
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

        String uuid = rnrTestNeedProducer.publish(need);


        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.needsRead.size() >= 1);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).pollDelay(200, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

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

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

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

        String uuid =rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

        assertEquals(ROBOT_1_PersonId, populatedNeed.personid);
        assertEquals(ROBOT_1_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_KnytningFinnes() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(KNYTNING_FINNES_1_PersonId)
                .pdlPersonider(Arrays.asList(KNYTNING_FINNES_1_PersonId))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_1_PersonId, populatedNeed.personid);
        assertEquals(KNYTNING_FINNES_1_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_KnytningFinnes_Flere_IDer_1() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_FLERE_ID_3_PersonId_1)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(KNYTNING_FINNES_FLERE_ID_3_PersonId_1)
                .pdlPersonider(Arrays.asList(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, KNYTNING_FINNES_FLERE_ID_3_PersonId_2))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        String uuid =rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, populatedNeed.personid);
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_NavId, populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_KnytningFinnes_Flere_IDer_2() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_FLERE_ID_3_PersonId_2)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(KNYTNING_FINNES_FLERE_ID_3_PersonId_1)
                .pdlPersonider(Arrays.asList(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, KNYTNING_FINNES_FLERE_ID_3_PersonId_2))
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_2, populatedNeed.personid);
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_NavId, populatedNeed.eksisterendeId);
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

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

        assertEquals(id, populatedNeed.personid);
        assertNull(populatedNeed.eksisterendeId);
    }

    @Test
    public void personOppslagId_Need_MedPDL_KnytningFinnes_Duplikat_NavIDer() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId)
                .pdlPersonider(Arrays.asList(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId))
                .responsProdusentVisited(true)
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId, populatedNeed.personid);
        assertEquals(KNYTNING_FINNES_DUPLIKAT_NavId_4_NavId_2, populatedNeed.eksisterendeId); // Dette er den med 'høyeste'/seneste navident
    }

    @Test
    public void personOppslagId_Need_PdlIkkeFunnet_TidligereAnsatt() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(null)
                .pdlPersonider(Collections.emptyList())
                .idGeneratorVisited(true)
                .generertId(null)
                .responsProdusentVisited(true)
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.personOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_personOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_1_PersonId, populatedNeed.personid);
        assertEquals(KNYTNING_FINNES_1_NavId, populatedNeed.eksisterendeId); // Selv om PDL feiler, bruk id som finnes
    }



    @Test
    public void pdlOppslag_Need_IdIkkeFunnet_TomRespons() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(KNYTNING_FINNES_1_PersonId)).thenReturn(Arrays.asList());

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(uuid);

        assertNull(populatedNeed.pdlGjeldendePersonid);
        assertTrue(populatedNeed.pdlPersonider.isEmpty());
    }

    @Test
    public void pdlOppslag_Need_IdIkkeFunnet_NullRespons() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(KNYTNING_FINNES_1_PersonId)).thenReturn(null);

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(uuid);

        assertNull(populatedNeed.pdlGjeldendePersonid);
        assertTrue(populatedNeed.pdlPersonider.isEmpty());
    }

    @Test
    public void pdlOppslag_Need_IdFunnet_1_ident() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(KNYTNING_FINNES_1_PersonId)).thenReturn(Arrays.asList(KNYTNING_FINNES_1_PersonId));

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_1_PersonId, populatedNeed.pdlGjeldendePersonid);
        assertEquals(1, populatedNeed.pdlPersonider.size());
        assertEquals(KNYTNING_FINNES_1_PersonId, populatedNeed.pdlPersonider.get(0));
    }

    @Test
    public void pdlOppslag_Need_IdFunnet_2_identer() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_FLERE_ID_3_PersonId_1)
                .identType(IdentType.STANDARD)
                .idGeneratorVisited(true)
                .personOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(KNYTNING_FINNES_FLERE_ID_3_PersonId_1)).thenReturn(Arrays.asList(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, KNYTNING_FINNES_FLERE_ID_3_PersonId_2));

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, populatedNeed.pdlGjeldendePersonid);
        assertEquals(2, populatedNeed.pdlPersonider.size());
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, populatedNeed.pdlPersonider.get(0));
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_2, populatedNeed.pdlPersonider.get(1));
    }

    @Test
    public void pdlOppslag_Need_IdFunnet_2_identer_v2() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_FLERE_ID_3_PersonId_2)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .build();

        Mockito.when(pdlService.getKnownPersonider(KNYTNING_FINNES_FLERE_ID_3_PersonId_2)).thenReturn(Arrays.asList(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, KNYTNING_FINNES_FLERE_ID_3_PersonId_2));

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.pdlOppslagVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_pdlOppslagVisited(uuid);

        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, populatedNeed.pdlGjeldendePersonid);
        assertEquals(2, populatedNeed.pdlPersonider.size());
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_1, populatedNeed.pdlPersonider.get(0));
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_PersonId_2, populatedNeed.pdlPersonider.get(1));
    }

    @Test
    public void navIdGenerator_personOppslagVisited_ikkeFunnet() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();


        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.idGeneratorVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_idGeneratorVisited(uuid);

        assertNotNull(populatedNeed.generertId);
        assertTrue(populatedNeed.generertId.startsWith("A"));
    }

    @Test
    public void navIdGenerator_personOppslagVisited_Funnet() {
        Need need = Need.builder()
                .idPrefix('A')
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .eksisterendeId(KNYTNING_FINNES_1_NavId)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .responsProdusentVisited(true)
                .build();


        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollDelay(200, MILLISECONDS).pollInterval(10, MILLISECONDS).until(() -> !rnrTestListener.idGeneratorVisited(uuid));

        // After 200 ms (poll delay), expect no id generated
        assertFalse(rnrTestListener.needsRead.stream().anyMatch(n -> n.isIdGeneratorVisited() && n.getPersonid().equals(uuid)));
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

        Mockito.when(navidRepository.getNextval()).thenReturn(123321L);

        String uuid = rnrTestNeedProducer.publish(need);


        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.idGeneratorVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_idGeneratorVisited(uuid);

        assertEquals("01019922222", populatedNeed.personid);
        assertNull(populatedNeed.eksisterendeId);
        assertNotNull(populatedNeed.generertId);
    }

    @Test
    public void responsProdusent_Robot_NyIdent() {
        Need need = Need.builder()
                .personid("Y1019900002")
                .identType(IdentType.ROBOT)
                .personOppslagVisited(true)
                .eksisterendeId(null)
                .idGeneratorVisited(true)
                .generertId("Y000002")
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited(uuid);

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("Y000002", respons.body.navident);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Robot_KnytningFinnes() {
        Need need = Need.builder()
                .personid("X1019900002")
                .identType(IdentType.ROBOT)
                .personOppslagVisited(true)
                .eksisterendeId("X000002")
                .build();

        String uuid =rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited(uuid);

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals("X000002", respons.body.navident);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Standard_KnytningFinnesIkke() {
        Need need = Need.builder()
                .personid("02019912345")
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .pdlPersonider(Arrays.asList("02019912345"))
                .pdlGjeldendePersonid("02019912345")
                .idGeneratorVisited(true)
                .generertId("C000001")
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited(uuid);

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("C000001", respons.body.navident);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Standard_KnytningFinnesIkke_PdlFunnetMenIkkeGjeldende() {
        Need need = Need.builder()
                .personid("01019912345")
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .pdlPersonider(Arrays.asList("01019912345", "11019912345"))
                .pdlGjeldendePersonid("11019912345")
                .idGeneratorVisited(true)
                .generertId("C000001")
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited(uuid);

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("C000001", respons.body.navident);
        assertNull(respons.body.feilmelding);
    }


    @Test
    public void responsProdusent_Standard_KnytningFinnes() {
        log.info("responsProdusent_Standard_KnytningFinnes()");
        Need need = Need.builder()
                .personid(KNYTNING_FINNES_1_PersonId)
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .pdlGjeldendePersonid(KNYTNING_FINNES_1_PersonId)
                .pdlPersonider(Arrays.asList(KNYTNING_FINNES_1_PersonId))
                .eksisterendeId(KNYTNING_FINNES_1_NavId)
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited(uuid);

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(KNYTNING_FINNES_1_NavId, respons.body.navident);
        assertNull(respons.body.feilmelding);
    }

    @Test
    public void responsProdusent_Standard_KnytningFinnesIkke_PdlIkkeFunnet() {
        Need need = Need.builder()
                .personid("01010100000")
                .identType(IdentType.STANDARD)
                .personOppslagVisited(true)
                .pdlOppslagVisited(true)
                .idGeneratorVisited(true)
                .generertId("C000001")
                .build();

        String uuid = rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.responsProdusentVisited(uuid));
        Need populatedNeed = rnrTestListener.getNeed_responsProdusentVisited(uuid);

        Respons respons = populatedNeed.respons;
        assertNotNull(respons);
        assertEquals(HttpStatus.NOT_FOUND, respons.httpStatus);
        assertEquals("C000001",respons.body.navident);
        assertNotNull(respons.body.feilmelding);
    }

}