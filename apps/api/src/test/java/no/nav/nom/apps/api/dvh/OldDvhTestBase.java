package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class OldDvhTestBase extends TestBase {

    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected DvhRepository dvhRepository;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
    }
}
