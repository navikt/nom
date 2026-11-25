package no.nav.nom.apps.testdata;

import no.nav.nom.apps.testdata.model.TestSett;
import no.nav.nom.apps.testdata.repository.OrgenhetEvtsrcMsgRepository;
import no.nav.nom.apps.testdata.repository.OrgenhetStateRepository;
import no.nav.nom.apps.testdata.repository.TestSettRepository;
import no.nav.nom.apps.testdata.service.OrgEnhetLederEvtsrcMsgService;
import no.nav.nom.apps.testdata.service.OrgEnhetLederStateService;
import no.nav.nom.apps.testdata.service.OrganiseringEvtsrcMsgService;
import no.nav.nom.apps.testdata.service.OrganiseringStateService;
import no.nav.nom.apps.testdata.service.OrgenhetEvtsrcMsgService;
import no.nav.nom.apps.testdata.service.OrgenhetStateService;
import no.nav.nom.apps.testdata.service.OrgtilknytningEvtsrcMsgService;
import no.nav.nom.apps.testdata.service.OrgtilknytningStateService;
import no.nav.nom.apps.testdata.service.RessursEvtsrcMsgService;
import no.nav.nom.apps.testdata.service.RessursStateService;
import no.nav.nom.apps.testdata.service.TestSettService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

@Testcontainers
@SpringBootTest(
    properties = {
        "unleash.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.invalid",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://issuer.invalid/jwks",
        "spring.security.oauth2.resourceserver.jwt.audiences=test-client",
        "nom.security.admin-group-id=test-admin",
        "nom.security.pre-authorized-apps=[{\"clientId\":\"test-caller\",\"name\":\"test\"}]"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class RepositoryDomainTestBase {
    @Container
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestSettService testSettService;
    @Autowired
    protected JdbcClient jdbcClient;
    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected TestSettRepository testSettRepository;
    @Autowired
    protected OrgenhetStateRepository orgenhetStateRepository;
    @Autowired
    protected OrgenhetEvtsrcMsgRepository orgenhetEvtsrcMsgRepository;

    @Autowired
    protected OrgenhetStateService orgenhetStateService;
    @Autowired
    protected OrgenhetEvtsrcMsgService orgenhetEvtsrcMsgService;
    @Autowired
    protected RessursStateService ressursStateService;
    @Autowired
    protected RessursEvtsrcMsgService ressursEvtsrcMsgService;
    @Autowired
    protected OrganiseringStateService organiseringStateService;
    @Autowired
    protected OrganiseringEvtsrcMsgService organiseringEvtsrcMsgService;
    @Autowired
    protected OrgEnhetLederStateService orgEnhetLederStateService;
    @Autowired
    protected OrgEnhetLederEvtsrcMsgService orgEnhetLederEvtsrcMsgService;
    @Autowired
    protected OrgtilknytningStateService orgtilknytningStateService;
    @Autowired
    protected OrgtilknytningEvtsrcMsgService orgtilknytningEvtsrcMsgService;

    @BeforeEach
    void resetDatabaseAndSecurityContext() {
        SecurityContextHolder.clearContext();
        jdbcClient.sql("DELETE FROM orgenhet_evtsrc_msg").update();
        jdbcClient.sql("DELETE FROM ressurs_evtsrc_msg").update();
        jdbcClient.sql("DELETE FROM organisering_evtsrc_msg").update();
        jdbcClient.sql("DELETE FROM orgenhet_leder_evtsrc_msg").update();
        jdbcClient.sql("DELETE FROM orgtilknytning_evtsrc_msg").update();
        jdbcClient.sql("DELETE FROM orgenhet_state").update();
        jdbcClient.sql("DELETE FROM ressurs_state").update();
        jdbcClient.sql("DELETE FROM organisering_state").update();
        jdbcClient.sql("DELETE FROM orgenhet_leder_state").update();
        jdbcClient.sql("DELETE FROM orgtilknytning_state").update();
        jdbcClient.sql("DELETE FROM test_sett").update();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected TestSett createSet() {
        return testSettService.opprett(new TestSett.Data("synthetic set", null));
    }

    protected void setActor(String ident) {
        var jwt = Jwt.withTokenValue("synthetic-token").header("alg", "RS256")
            .subject("synthetic-user").claim("NAVident", ident).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
