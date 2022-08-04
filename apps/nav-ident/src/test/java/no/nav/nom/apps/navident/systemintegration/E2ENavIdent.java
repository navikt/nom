package no.nav.nom.apps.navident.systemintegration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.NavIdent;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.api.dto.NavidentResponseDTO;
import no.nav.nom.apps.navident.common.redis.TestRedisConfiguration;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.eventsource.EventType;
import no.nav.nom.apps.navident.eventsource.SourceEvent;
import no.nav.nom.apps.navident.eventsource.SourceEventRepository;
import no.nav.nom.apps.navident.rnrservices.util.RnRTestListener;
import no.nav.nom.apps.navident.rnrservices.util.RnRTestNeedProducer;
import no.nav.nom.libs.test.SecurityTestUtils;
import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test klasse for å teste applikasjonen satt opp med integrasjon til andre løsninger.
 *
 * Redis - Embedded
 * Kafka - Embedded
 * Postgres - Embedded H2
 *
 * AAD - Test AAD
 * PDL - Test PDL
 */
@ActiveProfiles({"e2e-local", "log-console-mdc"})
@Slf4j

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = NavIdent.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

@Import({TestRedisConfiguration.class})
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })

@Disabled("Depends on external resources. Run explicitly")
public class E2ENavIdent {

    @MockBean RnRTestNeedProducer rnRTestNeedProducer;
    @MockBean RnRTestListener rnRTestListener;

    @LocalServerPort String port;

    @Autowired
    protected Map<String, Navident> navidenter;

    @Autowired
    protected OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    @Autowired
    protected SourceEventRepository sourceEventRepository;

    @Value("${AZURE_APP_CLIENT_ID}") String navIdentClientId;
    @Value("${e2e.aad.testJWT}") String testJWT;

    RestTemplate restTemplate = new RestTemplate();

    @BeforeAll
    public void init() {
        log.debug("Server port: %s".formatted(port));
        log.debug("Using JWT: %s".formatted(testJWT));

        assertTrue(validateJWTforTestUser(), "Is test JWT Token valid?");
    }

    @BeforeEach
    public void addMdc(TestInfo testInfo) {
        MDC.put(MDC_CALL_ID_KEY, UUID.randomUUID().toString());
        MDC.put(MDC_CONSUMER_ID_KEY, testInfo.getTestMethod().orElseThrow().getName());
    }

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    @SneakyThrows
    public void bestill_nyansatt_finnesIkkeIPDL() {
        String personid = "01019900001";
        int antNavidenter = navidenter.size();
        long antEventSourceInRepo = sourceEventRepository.count();
        assertEquals(0, navidenter.values().stream().filter( ident -> ident.is(personid)).count());

        ResponseEntity<NavidentResponseDTO> responseDTO = callNavIdent(personid, IdentType.STANDARD, "A");

        // Valider http response
        assertEquals(HttpStatus.CREATED, responseDTO.getStatusCode());
        String ident = responseDTO.getBody().getNavident();
        assertNotNull(ident);
        assertNotNull(responseDTO.getBody().getFeilmelding());

        Thread.sleep(1000); // Give some time for rnr and eventsource to catch up

        // Valider Domain objektene
        assertEquals(++antNavidenter, navidenter.size());
        assertTrue(navidenter.containsKey(ident));
        Navident navident = navidenter.get(ident);
        assertEquals(personid, navident.getGjeldendePersonid() );

        // Valider peristert SourceEvent
        assertEquals(++antEventSourceInRepo, sourceEventRepository.count());
        Optional<SourceEvent> sourceEvent = sourceEventRepository.findFirstByOrderByIdDesc();
        assertTrue(sourceEvent.isPresent());
        assertEquals(EventType.OPPRETT, sourceEvent.get().getEvent());
        assertEquals(ident, sourceEvent.get().getPayload().getNavident());
        assertEquals(personid, sourceEvent.get().getPayload().getPersonid());
    }

    @Test
    @SneakyThrows
    public void bestill_nyansatt_finnesIPDL() {
        String personid = "03048803430";
        int antNavidenter = navidenter.size();
        long antEventSourceInRepo = sourceEventRepository.count();
        assertEquals(0, navidenter.values().stream().filter( ident -> ident.is(personid)).count());

        ResponseEntity<NavidentResponseDTO> responseDTO = callNavIdent(personid, IdentType.STANDARD, "A");

        // Valider http response
        assertEquals(HttpStatus.CREATED, responseDTO.getStatusCode());
        String ident = responseDTO.getBody().getNavident();
        assertNotNull(ident);

        Thread.sleep(1000); // Give some time for rnr and eventsource to catch up

        // Valider Domain objektene
        assertEquals(++antNavidenter, navidenter.size());
        assertTrue(navidenter.containsKey(ident));
        Navident navident = navidenter.get(ident);
        assertEquals(personid, navident.getGjeldendePersonid() );

        // Valider peristert SourceEvent
        assertEquals(++antEventSourceInRepo, sourceEventRepository.count());
        Optional<SourceEvent> sourceEvent = sourceEventRepository.findFirstByOrderByIdDesc();
        assertTrue(sourceEvent.isPresent());
        assertEquals(EventType.OPPRETT, sourceEvent.get().getEvent());
        assertEquals(ident, sourceEvent.get().getPayload().getNavident());
        assertEquals(personid, sourceEvent.get().getPayload().getPersonid());
    }

    @Test
    @SneakyThrows
    public void bestill_tidligereAnsatt_finnesIPDL() {
        String personid = "07437746014";
        int antNavidenter = navidenter.size();
        long antEventSourceInRepo = sourceEventRepository.count();
        assertEquals(0, navidenter.values().stream().filter( ident -> ident.is(personid)).count());

        ResponseEntity<NavidentResponseDTO> responseDTO = callNavIdent(personid, IdentType.STANDARD, "A");
        String tidligereTildeltId = responseDTO.getBody().getNavident();

        responseDTO = callNavIdent(personid, IdentType.STANDARD, "A");
        // Valider http response
        assertEquals(HttpStatus.OK, responseDTO.getStatusCode());
        String id = responseDTO.getBody().getNavident();
        assertNotNull(id);

        Thread.sleep(1000); // Give some time for rnr and eventsource to catch up

        // Valider Domain objektene
        assertEquals(++antNavidenter, navidenter.size());
        assertTrue(navidenter.containsKey(id));
        Navident navident = navidenter.get(id);
        assertEquals(personid, navident.getGjeldendePersonid() );

        // Valider peristert SourceEvent
        assertEquals(antEventSourceInRepo+2, sourceEventRepository.count());
        Optional<SourceEvent> sourceEvent = sourceEventRepository.findFirstByOrderByIdDesc();
        assertTrue(sourceEvent.isPresent());
        assertEquals(EventType.OPPRETT, sourceEvent.get().getEvent());
        assertEquals(id, sourceEvent.get().getPayload().getNavident());
        assertEquals(personid, sourceEvent.get().getPayload().getPersonid());
    }

    @Test
    @SneakyThrows
    public void bestill_tidligereAnsattAnnenPersonid_finnesIPDL() {
        String gammelPersonid = "65108148858";
        String nyPersonid = "11108123097";
        int antNavidenter = navidenter.size();
        long antEventSourceInRepo = sourceEventRepository.count();
        assertEquals(0, navidenter.values().stream().filter( ident -> ident.is(gammelPersonid)).count());
        assertEquals(0, navidenter.values().stream().filter( ident -> ident.is(nyPersonid)).count());

        ResponseEntity<NavidentResponseDTO> responseDTO = callNavIdent(gammelPersonid, IdentType.STANDARD, "A");
        String tidligereTildeltId = responseDTO.getBody().getNavident();
        // Valider http response
        assertEquals(HttpStatus.CREATED, responseDTO.getStatusCode());
        String opprinneligNavid = responseDTO.getBody().getNavident();
        assertNotNull(opprinneligNavid);

        responseDTO = callNavIdent(nyPersonid, IdentType.STANDARD, "A");
        // Valider http response
        assertEquals(HttpStatus.OK, responseDTO.getStatusCode());
        String navid = responseDTO.getBody().getNavident();
        assertNotNull(navid);
        assertEquals(opprinneligNavid, navid);

        Thread.sleep(1000); // Give some time for rnr and eventsource to catch up

        // Valider Domain objektene
        assertEquals(++antNavidenter, navidenter.size());
        assertTrue(navidenter.containsKey(navid));
        Navident navident = navidenter.get(navid);
        assertEquals(nyPersonid, navident.getGjeldendePersonid() );
        assertTrue(navident.is(gammelPersonid));

        // Valider peristert SourceEvent
        assertEquals(antEventSourceInRepo+2, sourceEventRepository.count());
        Optional<SourceEvent> sourceEvent = sourceEventRepository.findFirstByOrderByIdDesc();
        assertTrue(sourceEvent.isPresent());
        assertEquals(EventType.OPPRETT, sourceEvent.get().getEvent());
        assertEquals(navid, sourceEvent.get().getPayload().getNavident());
        assertEquals(nyPersonid, sourceEvent.get().getPayload().getPersonid());
    }

    @Test
    @SneakyThrows
    public void bestill_robot() {
        String personid = "112233";
        int antNavidenter = navidenter.size();
        long antEventSourceInRepo = sourceEventRepository.count();
        assertEquals(0, navidenter.values().stream().filter( ident -> ident.is(personid)).count());

        ResponseEntity<NavidentResponseDTO> responseDTO = callNavIdent(personid, IdentType.ROBOT, "X");

        // Valider http response
        assertEquals(HttpStatus.CREATED, responseDTO.getStatusCode());
        String ident = responseDTO.getBody().getNavident();
        assertNotNull(ident);

        Thread.sleep(1000); // Give some time for rnr and eventsource to catch up

        // Valider Domain objektene
        assertEquals(++antNavidenter, navidenter.size());
        assertTrue(navidenter.containsKey(ident));
        Navident navident = navidenter.get(ident);
        assertEquals(personid, navident.getGjeldendePersonid() );
        assertTrue(navident.getNavident().startsWith("X"));

        // Valider peristert SourceEvent
        assertEquals(++antEventSourceInRepo, sourceEventRepository.count());
        Optional<SourceEvent> sourceEvent = sourceEventRepository.findFirstByOrderByIdDesc();
        assertTrue(sourceEvent.isPresent());
        assertEquals(EventType.OPPRETT, sourceEvent.get().getEvent());
        assertEquals(ident, sourceEvent.get().getPayload().getNavident());
        assertEquals(personid, sourceEvent.get().getPayload().getPersonid());
    }

    private boolean validateJWTforTestUser() {
        String tokenGenerationInIDA = "Create a valid token in IDA (https://ida.intern.nav.no/) under 'AAD On-Behalf-Of Token' bu first logging in to AAD and then 'generer on-behalf-of Token' for client id '%s'".formatted(navIdentClientId);
        try {
            Jwt jwt = JwtUtils.jwtDecoder(oAuth2ResourceServerProperties).decode(testJWT);
            if (jwt.getExpiresAt().isBefore(Instant.now().plusSeconds(60 * 5))) {
                log.debug("JWT provided in local parameter 'e2e.aad.testJWT' expires soon. " + tokenGenerationInIDA);
            }
        } catch (JwtValidationException e) {
            log.error("Invalid JWT provided in local parameter 'e2e.aad.testJWT'." + tokenGenerationInIDA, e);
            return false;
        }
        return true;
    }

    @SneakyThrows
    private ResponseEntity<NavidentResponseDTO> callNavIdent(String personId, IdentType type, String preifx) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_CALL_ID_KEY, MDC.get(MDC_CALL_ID_KEY));
        headers.set(HEADER_CONSUMER_ID_KEY, MDC.get(MDC_CONSUMER_ID_KEY));
        headers.setBearerAuth(testJWT);

        NavidentRequestDTO reqDTO = NavidentRequestDTO.builder()
                .personid(personId)
                .identType(type)
                .identPrefix(preifx)
                .build();
        HttpEntity<NavidentRequestDTO> request = new HttpEntity<>(reqDTO, headers);

        String url = "http://localhost:%s/navident".formatted(port);

        log.debug("Url: %s".formatted(url));
        log.debug("Request: %s".formatted(request));

        ResponseEntity<NavidentResponseDTO> result = restTemplate.postForEntity(url, request, NavidentResponseDTO.class);

        log.debug("Response: %s".formatted(result));
        return result;
    }
}
