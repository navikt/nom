package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.domain.OrgenhetService;
import no.nav.nom.apps.api.domain.RessursService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DvhTestBase extends TestBase {

    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected RessursService ressursService;

    @Autowired
    protected OrgenhetService orgenhetService;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected DvhRepository dvhRepository;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
    }

    protected void loadFromDvh() {
        dvhRestConsumer.loadFromDvh();
    }
}
