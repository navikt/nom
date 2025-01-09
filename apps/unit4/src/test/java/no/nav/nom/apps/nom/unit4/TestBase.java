package no.nav.nom.apps.nom.unit4;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"org.nom.ressurs-eventsource", "org.nom.ressurs-eventsource-dlt"}, controlledShutdown = true)
@ExtendWith({SpringExtension.class})
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@AutoConfigureWireMock(port = 0)
@Slf4j
public class TestBase {

    @Autowired
    protected WebTestClient webTestClient;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected MockOAuth2Server oAuth2Server;
    
}
