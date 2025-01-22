package no.nav.nom.libs.state.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.StateTopicConsumerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Import({
    KafkaTestFactoryConfiguration.class
    , NomStateRessursListener.class
    , RessursStateAutoconfiguration.class
})
public class RessursStateTopicConsumerTest  extends StateTopicConsumerTestBase<RessursState> {

    public RessursStateTopicConsumerTest(NomStateRessursListener nomStateRessursListener) {
        super(nomStateRessursListener);
        stateTopic = "org.nom.intern-ressurs-state";

        testMessage = RessursState.builder()
            .fid("FID_1")
            .navident("A000001")
            .personident("12345678901")
            .visningsnavn(new Visningsnavn("John", Optional.empty(), "Doe"))
            .startdato(Optional.of(LocalDate.of(2022, 1, 1)))
            .sektor(Sektor.NAV_STATLIG)
            .build();
    }

    @Autowired
    RessursStateRepository ressursStateRepository;

    @BeforeEach
    public void emptyRepo() {
        ressursStateRepository.clear();
    }

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-ressurs-state")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(2);

        sendMessage("FID_1", testMessage);

        sendMessage("FID_2",
            RessursState.builder()
                .fid("FID_2")
                .navident("A000002")
                .personident("12345678902")
                .visningsnavn(new Visningsnavn("Jane", Optional.empty(), "Doe"))
                .startdato(Optional.of(LocalDate.of(2022, 1, 1)))
                .sektor(Sektor.NAV_STATLIG)
                .primaryFysiskBrukersted(Optional.of("1234"))
                .fysiskeBrukersteder(List.of("1234", "5678"))
                .primaryTelefonnummer(Optional.of("12345678"))
                .build()
        );

        alleMeldingerLest.await();

        assertEquals(2, ressursStateRepository.count());
        assertEquals("Doe", ressursStateRepository.findByFid("FID_1").get().visningsnavn().etternavn());
    }
}
