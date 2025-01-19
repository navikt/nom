package no.nav.nom.libs.state.orgenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.StateTopicConsumerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Import({
    KafkaTestFactoryConfiguration.class
    , NomStateOrgEnhetListener.class
    , OrgEnhetStateAutoconfiguration.class
})
public class OrgEnhetStateTopicConsumerTest extends StateTopicConsumerTestBase<OrgEnhetState> {

    public OrgEnhetStateTopicConsumerTest(NomStateOrgEnhetListener nomStateOrgEnhetListener) {
        super(nomStateOrgEnhetListener);
        stateTopic = "org.nom.intern-orgenhet-state";

        testMessage = OrgEnhetState.builder()
            .fid("FID_X")
            .id("ID_1")
            .agressoId(Optional.of("001"))
            .agressoOrgNivaa(Optional.of("ORGENHET"))
            .navn("NAVNET")
            .gyldigFom(LocalDate.of(2021, 1, 1))
            .uniqueAgressoId(Optional.of("001-NAVNET-ORGENHET"))
            .build();
    }

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-orgenhet-state")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(1);

        sendMessage("FID_1", testMessage);

        alleMeldingerLest.await();
    }
}
