package no.nav.nom.apps.dvh;

import no.nav.nom.apps.proxy.common.AppProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock({
    @ConfigureWireMock(
        filesUnderDirectory = "src/test/resources")
})
@TestPropertySource(properties = "wiremock.server.disable-chunked-encoding=true")
@AutoConfigureWebTestClient
public class TestBase {

    @Autowired
    protected AppProperties properties;

}
