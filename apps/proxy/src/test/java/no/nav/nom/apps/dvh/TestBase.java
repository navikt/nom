package no.nav.nom.apps.dvh;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles({"test", "log-console-error"})
@WireMockStubs
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestBase {

    @Autowired
    protected AppProperties properties;

}
