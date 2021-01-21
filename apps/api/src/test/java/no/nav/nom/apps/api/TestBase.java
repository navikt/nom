package no.nav.nom.apps.api;

import no.nav.nom.apps.api.dvh.DvhRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles({"test", "log-console-error"})
@AutoConfigureWireMock(port = 0)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TestBase {

    @Autowired
    private DvhRepository dvhRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void reset() {
        dvhRepository.clearRepository();
    }
}
