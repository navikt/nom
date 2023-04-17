package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DvhTestBase extends TestBase {

    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected DvhRepository dvhRepository;

    @Autowired
    protected OrgtilknytningService orgtilknytningService;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
    }

    protected void loadFromDvh() {
        dvhRestConsumer.loadRessurserOgOrgEnheter();
        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();
    }
}
