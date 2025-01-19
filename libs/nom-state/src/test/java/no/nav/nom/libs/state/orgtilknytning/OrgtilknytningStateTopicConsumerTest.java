package no.nav.nom.libs.state.orgtilknytning;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.StateTopicConsumerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Import({
    KafkaTestFactoryConfiguration.class
    , NomStateOrgtilknytningListener.class
    , OrgtilknytningStateAutoconfiguration.class
})
public class OrgtilknytningStateTopicConsumerTest extends StateTopicConsumerTestBase<OrgtilknytningState> {

    public OrgtilknytningStateTopicConsumerTest(NomStateOrgtilknytningListener nomStateOrgtilknytningListener) {
        super(nomStateOrgtilknytningListener);
        stateTopic = "org.nom.intern-orgtilknytning-state";

        testMessage = OrgtilknytningState.builder()
            .fid("FID_1")
            .navident("Z999999")
            .orgEnhetId("ab123c")
            .gyldigFom(LocalDate.now().minusMonths(3))
            .gyldigTom(Optional.empty())
            .erDagligOppfolging(true)
            .orgtilknytningType(Optional.of(Tilknytningtype.INGEN))
            .build();
    }

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-orgtilknytning-state")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(1);

        sendMessage("FID_1", testMessage);

        alleMeldingerLest.await();
    }
}
