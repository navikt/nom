package no.nav.nom.apps.navident;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.redis.TestRedisConfiguration;
import no.nav.nom.libs.test.RsaStub;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@ActiveProfiles({"test", "log-console"})
@WireMockStubs(RsaStub.class)
@Slf4j
@SpringBootTest
@Import(TestRedisConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class SpringBootTestBase {
    @MockBean DataSource dataSource;

}
