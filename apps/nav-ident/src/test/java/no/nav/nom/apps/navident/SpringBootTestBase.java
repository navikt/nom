package no.nav.nom.apps.navident;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.redis.TestRedisConfiguration;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import no.nav.nom.libs.utils.common.mdc.RestMdcAutoconfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.util.UUID;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_CALL_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_CONSUMER_ID_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles({"test"})
@Slf4j
@SpringBootTest
@Import({TestRedisConfiguration.class, RestMdcAutoconfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@AutoConfigureWireMock(port = 0)
public abstract class SpringBootTestBase {
    
    @MockBean DataSource dataSource;

    @MockBean
    protected OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
    
    @MockBean
    Unleash unleash;

    @BeforeEach
    public void addMdc(TestInfo testInfo) {
        MDC.put(MDC_CALL_ID_KEY, UUID.randomUUID().toString());
        MDC.put(MDC_CONSUMER_ID_KEY, testInfo.getTestMethod().orElseThrow().getName());
        
        when(unleash.isEnabled(any())).thenReturn(true);
    }

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }
}
