package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.MockOAuth2ServerInitializer;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@ActiveProfiles("test")
@ConfigurationPropertiesScan
@SpringBootTest
@ContextConfiguration(classes= DvhTestConfig.class, initializers = MockOAuth2ServerInitializer.class)
@AutoConfigureWireMock(port = 0)
public class DvhTestBase {
    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected DvhRepository dvhRepository;

    @Autowired
    protected RessursStateRepository ressursStateRepository;

    @MockBean
    protected Unleash unleash;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
    }
}
