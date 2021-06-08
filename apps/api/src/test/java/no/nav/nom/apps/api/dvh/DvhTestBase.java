package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.domain.OrgenhetRepository;
import no.nav.nom.apps.api.domain.RessursRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DvhTestBase extends TestBase {

    @Autowired
    protected DvhRestConsumer dvhRestConsumer;

    @Autowired
    protected RessursRepository ressursRepository;

    @Autowired
    protected OrgenhetRepository orgenhetRepository;

    @Autowired
    protected KafkaUtils kafkaUtils;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    private DvhRepository dvhRepository;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
        ressursRepository.clearRepository();
        orgenhetRepository.clearRepository();
    }

    protected void loadFromDvhAndUpdateRepo() {
        dvhRestConsumer.loadFromDvh();
        var records = getRecordsFromKafka(true);
        kafkaUtils.sendHendelseFromKafkaMock(records);
    }

    protected void simulateSyncedWithKafka() {
        kafkaUtils.sendHendelseFromKafkaMock();
    }

}
