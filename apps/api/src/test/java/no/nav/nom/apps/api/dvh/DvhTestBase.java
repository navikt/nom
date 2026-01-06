package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.MasseflyttOrgtilknytningerService;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@Slf4j
@ActiveProfiles("test")
@ConfigurationPropertiesScan
@SpringBootTest
@ContextConfiguration(classes= DvhTestConfig.class, initializers = MockOAuth2ServerInitializer.class)
@EnableWireMock({
    @ConfigureWireMock(
        filesUnderDirectory = "src/test/resources")
})
public class DvhTestBase {
    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected DvhRepository dvhRepository;

    @MockitoBean
    protected TelefonService telefonService;

    @MockitoBean
    protected OrgEnhetLederService orgEnhetLederService;

    @Autowired
    protected RessursStateRepository ressursStateRepository;

    @MockitoBean
    protected Unleash unleash;

    @MockitoBean
    protected MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService;

    @MockitoBean
    protected OrganiseringStateRepository organiseringStateRepository;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
    }
}
