package no.nav.nom.apps.ui;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import no.nav.nom.apps.ui.common.AppProperties;
import no.nav.nom.libs.test.RsaStub;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@WireMockStubs(RsaStub.class)
@ActiveProfiles({"test", "log-console"})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TestBase {

    @Autowired
    protected AppProperties properties;

}
