package no.nav.nom.libs.state.organisering;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.StateTopicConsumerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

@Slf4j
@Import({
    KafkaTestFactoryConfiguration.class
    , NomStateOrganiseringListener.class
    , OrganiseringStateAutoconfiguration.class
})
public class OrganiseringStateTopicConsumerTest extends StateTopicConsumerTestBase<OrganiseringState> {

    public OrganiseringStateTopicConsumerTest(NomStateOrganiseringListener nomStateOrganiseringListener) {
        super(nomStateOrganiseringListener);
        stateTopic = "org.nom.intern-organisering-state";

        testMessage = OrganiseringState.builder()
            .fid("FID_1")
            .id("ID_2")
            .idOver("ID_1")
            .navn("NAVN_1")
            .navnOver("NAVN_2")
            .gyldigFom(LocalDate.of(2021, 1, 1))
            .build();
    }

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-organisering-state")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(1);

        sendMessage("FID_1", testMessage);

        alleMeldingerLest.await();
    }
}
