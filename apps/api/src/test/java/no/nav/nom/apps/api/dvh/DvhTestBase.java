package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetPosgresqlService;
import no.nav.nom.apps.api.ressurs.implementations.RessursService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DvhTestBase extends TestBase {

    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected RessursService ressursService;

    @Autowired
    protected OrgenhetPosgresqlService orgenhetService;

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
