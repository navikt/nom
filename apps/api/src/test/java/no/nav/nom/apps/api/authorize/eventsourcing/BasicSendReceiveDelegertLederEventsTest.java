package no.nav.nom.apps.api.authorize.eventsourcing;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.AvsluttDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.FornyDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.OpprettDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.state.OldDelegertLederStateRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
public class BasicSendReceiveDelegertLederEventsTest extends KafkaTestBase {
    @Autowired
    AuthorizeCommandService authorizeCommandService;

    @Autowired
    OldDelegertLederStateRepositoryImpl delegertLederStateRepository;

    @BeforeEach
    public void resetDomain() {
        delegertLederStateRepository.clear();
    }

    @Test
    @DisplayName("Send og les DelegertAuthorizationOpprettetEventV1")
    public void test_0001() throws InterruptedException, TimeoutException {
        registerMessagesToSend(1);

        authorizeCommandService.opprettDelegertAuthorization(new OpprettDelegertAuthorization("A000001", "B0000001", "ab123c", LocalDate.now(), LocalDate.now().plusYears(1), null));

        awaitAllMessages();

        var delegeringer = delegertLederStateRepository.findByNavident("A000001");

        assertEquals(1, delegeringer.size());
        var delegering =  delegeringer.iterator().next();
        assertEquals("A000001", delegering.til());
        assertEquals("ab123c", delegering.orgenhetId());
    }

    @Test
    @DisplayName("Send og les Opprett og Avslutt")
    public void test_0002() throws InterruptedException, TimeoutException {
        registerMessagesToSend(2);

        var opprettAuthorization = new OpprettDelegertAuthorization("A000002", "B0000001", "ab123c", LocalDate.now(), LocalDate.now().plusYears(1), null);
        var fid = authorizeCommandService.opprettDelegertAuthorization(opprettAuthorization);
        authorizeCommandService.avsluttDelegertAuthorization(fid, new AvsluttDelegertAuthorization("A000002", "B0000001", "ab123c", opprettAuthorization.gyldigFom(), opprettAuthorization.gyldigTom(), opprettAuthorization.gyldigTom().minusMonths(2), null));

        awaitAllMessages();

        var delegeringer = delegertLederStateRepository.findByNavident("A000002");

        assertEquals(1, delegeringer.size());
        var delegering =  delegeringer.iterator().next();
        assertEquals("A000002", delegering.til());
        assertEquals("ab123c", delegering.orgenhetId());
        assertEquals(LocalDate.now().plusMonths(10), delegering.gyldigTom());
    }

    @Test
    @DisplayName("Send og les Opprett, Forny og Avslutt")
    public void test_0003() throws InterruptedException, TimeoutException {
        registerMessagesToSend(3);

        var opprettAuthorization = new OpprettDelegertAuthorization("A000003", "B0000001", "ab123c", LocalDate.now(), LocalDate.now().plusYears(1), null);
        var fid = authorizeCommandService.opprettDelegertAuthorization(opprettAuthorization);
        authorizeCommandService.fornyDelegertAuthorization(fid, new FornyDelegertAuthorization("A000003", "B0000001", "ab123c", opprettAuthorization.gyldigFom(), opprettAuthorization.gyldigTom(), opprettAuthorization.gyldigTom().minusMonths(2), null));
        authorizeCommandService.avsluttDelegertAuthorization(fid, new AvsluttDelegertAuthorization("A000003", "B0000001", "ab123c", opprettAuthorization.gyldigFom(), opprettAuthorization.gyldigTom(), opprettAuthorization.gyldigTom().minusMonths(4), null));

        awaitAllMessages();

        var delegeringer = delegertLederStateRepository.findByNavident("A000003");

        assertEquals(1, delegeringer.size());
        var delegering =  delegeringer.iterator().next();
        assertEquals("A000003", delegering.til());
        assertEquals("ab123c", delegering.orgenhetId());
        assertEquals(LocalDate.now().plusMonths(8), delegering.gyldigTom());
    }
}
