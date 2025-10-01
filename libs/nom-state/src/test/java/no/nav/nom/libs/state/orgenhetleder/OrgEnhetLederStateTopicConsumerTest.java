package no.nav.nom.libs.state.orgenhetleder;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.libs.state.StateTopicConsumerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

@Slf4j
@Import({
    KafkaTestFactoryConfiguration.class,
    NomStateOrgEnhetLederListener.class,
    OrgEnhetLederStateAutoconfiguration.class
})
public class OrgEnhetLederStateTopicConsumerTest extends StateTopicConsumerTestBase<OrgEnhetLederState> {

    public OrgEnhetLederStateTopicConsumerTest(NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener) {
        super(nomStateOrgEnhetLederListener);
        stateTopic = "org.nom.intern-orgenhet-leder-state";

        testMessage = OrgEnhetLederState.builder()
            .fid("FID_1")
            .navident("Z999999")
            .orgEnhetId("ab123c")
            .startdato(LocalDate.now())
            .sluttdato(LocalDate.now().plusMonths(3))
            .sistEndretAv("S123456")
            .build();
    }

    @Autowired
    private OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    @Test
    @DisplayName("Test at vi kan konsumere meldinger fra org.nom.intern-orgenhet-leder-state")
    public void testKonsumerMeldingerFraOrgEnhetLederEventSource() throws InterruptedException {
        setMessageCount(1);

        sendMessage("FID_1", testMessage);

        alleMeldingerLest.await();
        /*Optional<OrgEnhetLederState> orgEnhetLederState = orgEnhetLederStateRepository.findByFid("FID_001");
        assertEquals("A000001", orgEnhetLederState.get().navident());
        assertEquals("orgEnhetId-123", orgEnhetLederState.get().orgEnhetId());
        assertEquals(LocalDate.now(), orgEnhetLederState.get().startdato());
        assertEquals(LocalDate.now(), orgEnhetLederState.get().sluttdato());*/
    }
}
