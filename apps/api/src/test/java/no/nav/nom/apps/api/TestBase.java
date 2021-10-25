package no.nav.nom.apps.api;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.repository.hendelse.Hendelse;
import no.nav.nom.apps.api.repository.hendelse.HendelseRepository;
import org.junit.ClassRule;
import no.nav.nom.libs.test.RsaStub;
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

import java.util.List;

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
    private DvhRepository dvhRepository;

    @Autowired
    private HendelseRepository hendelseRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void reset() {
        dvhRepository.clearRepository();
        hendelseRepository.deleteAll();
    }

    protected void saveHendelser(Hendelse... hendelser){
        hendelseRepository.saveAll(List.of(hendelser));
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
}
