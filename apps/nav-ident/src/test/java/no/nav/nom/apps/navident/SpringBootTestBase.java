package no.nav.nom.apps.navident;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.test.RsaStub;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@ActiveProfiles({"test", "log-console"})
@WireMockStubs(RsaStub.class)
@Slf4j
@SpringBootTest
public abstract class SpringBootTestBase {
    @MockBean DataSource dataSource;

}
