package no.nav.nom.apps.api;

import com.nimbusds.jwt.JWTClaimsSet;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.ClassRule;
import no.nav.nom.libs.test.RsaStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles({"test", "log-console-error"})
@WireMockStubs(RsaStub.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {TestBase.Initializer.class})
public abstract class TestBase {

    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = CustomPostgreSQLContainer.getInstance();

    static {
        postgreSQLContainer.start();
    }

    @Autowired
    private SecurityTestUtils securityTestUtils;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    private GenericStorage genericStorage;

    @Autowired
    protected PdlUtils pdlUtils;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
        pdlUtils.reset();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void reset() {
        dvhRepository.clearRepository();
        genericStorage.deleteAll();
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    protected JWTClaimsSet.Builder validJwtWithAzpClaim(){
        val jwtClaimsBuilder = securityTestUtils.validJwtClaims();
        val subject = jwtClaimsBuilder.getClaims().get("sub");
        return securityTestUtils.validJwtClaims()
                .claim("azp", subject);
    }
}
