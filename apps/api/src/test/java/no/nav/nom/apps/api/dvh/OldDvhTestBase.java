package no.nav.nom.apps.api.dvh;

import no.nav.nom.apps.api.TestBase;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

public abstract class OldDvhTestBase extends TestBase {

    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected JsonMapper om;

    @Autowired
    protected DvhRepository dvhRepository;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
    }
}
