package no.nav.nom.apps.navident;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.redis.TestRedisConfiguration;
import no.nav.nom.libs.test.RsaStub;
import no.nav.nom.libs.utils.common.mdc.RestMdcAutoconfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.UUID;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.*;

@ActiveProfiles({"test"})
@WireMockStubs(RsaStub.class)
@Slf4j
@SpringBootTest
@Import({TestRedisConfiguration.class, RestMdcAutoconfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class SpringBootTestBase {
    @MockBean DataSource dataSource;

    @BeforeEach
    public void addMdc(TestInfo testInfo) {
        MDC.put(MDC_CALL_ID_KEY, UUID.randomUUID().toString());
        MDC.put(MDC_CONSUMER_ID_KEY, testInfo.getTestMethod().orElseThrow().getName());
    }

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }
}
