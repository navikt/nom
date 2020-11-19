package no.nav.nom.apps.dvh.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.dvh.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.ConsumerFactory;

abstract class StateTestBase extends TestBase {

    @SpyBean
    StateLoader loader;

    StateRepository repository;

    @Autowired
    ConsumerFactory<String, String> consumerFactory;

    @Autowired
    ObjectMapper mapper;

    @BeforeEach
    void setup() {
        Mockito.reset(loader, consumerFactory);
        // Oppretter ny for hver gang siden det må valideres på intern state, noe som blir vanskelig med singletons fra Spring.
        repository = new StateRepository(loader);
    }
}
