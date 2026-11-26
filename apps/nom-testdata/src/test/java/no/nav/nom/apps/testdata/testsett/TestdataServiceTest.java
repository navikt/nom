package no.nav.nom.apps.testdata.testsett;

import no.nav.nom.apps.testdata.RepositoryDomainTestBase;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetEvtsrcMsg;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetState;
import no.nav.nom.apps.testdata.testsett.locking.TestSettPessimisticLocking;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestdataServiceTest extends RepositoryDomainTestBase {
    private static final Instant START = Instant.parse("2026-01-14T10:00:00Z");
    private static final LocalDateTime STATE_TIME = LocalDateTime.of(2026, 1, 14, 12, 0);

    @Autowired
    private TestSettPessimisticLocking testSettPessimisticLocking;

    @Test
    void readsCompleteSetAndUpdatesMetadataWithoutChangingCreationAudit() {
        setActor("synthetic-creator");
        var sett = testSettService.opprett(new TestSett.Data("Synthetic set", "description"));
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var melding = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));

        assertThat(sett.audit().opprettetAv()).isEqualTo("synthetic-creator");
        assertThat(state.audit().opprettetAv()).isEqualTo("synthetic-creator");
        assertThat(melding.audit().opprettetAv()).isEqualTo("synthetic-creator");


        setActor("synthetic-editor");
        var updated = testSettService.endre(sett.id(), new TestSett.Data("Updated set", "new description"));
        var complete = testSettService.hentKomplett(sett.id());

        assertThat(updated.audit().opprettet()).isEqualTo(sett.audit().opprettet());
        assertThat(updated.audit().opprettetAv()).isEqualTo("synthetic-creator");
        assertThat(updated.audit().sistEndretAv()).isEqualTo("synthetic-editor");
        assertThat(complete.testSett()).isEqualTo(updated);
        assertThat(complete.states()).containsExactly(state);
        assertThat(complete.meldinger()).containsExactly(melding);
        assertThat(testSettService.finnAlle()).containsExactly(updated);
    }

    @Test
    void updatesStateAndMessageWithoutChangingTheirCreationAudit() {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var melding = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));

        setActor("synthetic-editor");

        var updatedState = orgenhetStateService.endre(sett.id(), state.id(), stateData(4, STATE_TIME.plusDays(1)));
        var updatedMessage = orgenhetEvtsrcMsgService.endre(sett.id(), melding.id(), messageData(null, 5));

        assertThat(updatedState.audit().opprettet()).isEqualTo(state.audit().opprettet());
        assertThat(updatedState.audit().opprettetAv()).isEqualTo("nom-testdata");
        assertThat(updatedState.audit().sistEndretAv()).isEqualTo("synthetic-editor");
        assertThat(updatedMessage.audit().opprettet()).isEqualTo(melding.audit().opprettet());
        assertThat(updatedMessage.audit().opprettetAv()).isEqualTo("nom-testdata");
        assertThat(updatedMessage.audit().sistEndretAv()).isEqualTo("synthetic-editor");
        assertThat(updatedMessage.data().orgenhetStateId()).isNull();
    }

    @Test
    void sortsOnlyBySortIdxIncludingHistoryWithReverseTimestamps() {
        var sett = createSet();
        var first = orgenhetStateService.opprett(sett.id(), stateData(10, STATE_TIME.plusDays(2)));
        var second = orgenhetStateService.opprett(sett.id(), stateData(20, STATE_TIME));
        var third = orgenhetStateService.opprett(sett.id(), stateData(30, STATE_TIME));
        var secondMessage = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(second.id(), 20));
        var firstMessage = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(first.id(), 10));

        assertThat(orgenhetStateService.finnAlle(sett.id())).containsExactly(first, second, third);
        assertThat(orgenhetStateService.finnForOrgenhet(sett.id(), "synthetic-unit")).containsExactly(first, second, third);
        assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).containsExactly(firstMessage, secondMessage);
        assertThat(testSettService.hentKomplett(sett.id()).states()).containsExactly(first, second, third);
    }

    @Test
    void databaseRejectsStateReferenceToAnotherSet() {
        var firstSet = createSet();
        var otherSet = createSet();
        var state = orgenhetStateService.opprett(firstSet.id(), stateData(0, STATE_TIME));
        var message = orgenhetEvtsrcMsgService.opprett(otherSet.id(), messageData(null, 0));

        assertThatThrownBy(() -> orgenhetEvtsrcMsgService.opprett(otherSet.id(), messageData(state.id(), 1)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> orgenhetEvtsrcMsgService.endre(otherSet.id(), message.id(), messageData(state.id(), 0)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(orgenhetEvtsrcMsgService.finnAlle(firstSet.id())).isEmpty();
        assertThat(orgenhetEvtsrcMsgService.finnAlle(otherSet.id())).containsExactly(message);
    }

    @Test
    void childOperationsCannotTargetRowsInAnotherSet() {
        var sett = createSet();
        var otherSet = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var message = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));

        assertThatThrownBy(() -> orgenhetStateService.hent(otherSet.id(), state.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgenhetStateService.endre(otherSet.id(), state.id(), stateData(3, STATE_TIME)))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgenhetStateService.slett(otherSet.id(), state.id(), true)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgenhetEvtsrcMsgService.hent(otherSet.id(), message.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgenhetEvtsrcMsgService.endre(otherSet.id(), message.id(), messageData(null, 3)))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgenhetEvtsrcMsgService.slett(otherSet.id(), message.id())).isInstanceOf(NoSuchElementException.class);

        assertThat(orgenhetStateService.hent(sett.id(), state.id())).isEqualTo(state);
        assertThat(orgenhetEvtsrcMsgService.hent(sett.id(), message.id())).isEqualTo(message);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletesOneStateWithExplicitMessagePolicy(boolean deleteMessages) {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var retainedState = orgenhetStateService.opprett(sett.id(), stateData(1, STATE_TIME));
        var linked = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));
        var other = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(retainedState.id(), 1));
        var standalone = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 2));
        setActor("synthetic-editor");

        orgenhetStateService.slett(sett.id(), state.id(), deleteMessages);

        assertThat(orgenhetStateService.finnAlle(sett.id())).containsExactly(retainedState);
        assertThat(orgenhetEvtsrcMsgService.hent(sett.id(), other.id())).isEqualTo(other);
        assertThat(orgenhetEvtsrcMsgService.hent(sett.id(), standalone.id())).isEqualTo(standalone);
        if (deleteMessages) {
            assertThatThrownBy(() -> orgenhetEvtsrcMsgService.hent(sett.id(), linked.id())).isInstanceOf(NoSuchElementException.class);
        } else {
            var detached = orgenhetEvtsrcMsgService.hent(sett.id(), linked.id());
            assertThat(detached.data().orgenhetStateId()).isNull();
            assertThat(detached.data().evtsrcMessage()).isEqualTo(linked.data().evtsrcMessage());
            assertThat(detached.audit().opprettet()).isEqualTo(linked.audit().opprettet());
            assertThat(detached.audit().opprettetAv()).isEqualTo(linked.audit().opprettetAv());
            assertThat(detached.audit().sistEndretAv()).isEqualTo("synthetic-editor");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletesStateWithoutMessagesWithEitherPolicy(boolean deleteMessages) {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));

        orgenhetStateService.slett(sett.id(), state.id(), deleteMessages);

        assertThat(orgenhetStateService.finnAlle(sett.id())).isEmpty();
        assertThat(testSettService.hent(sett.id())).isEqualTo(sett);
    }

    @Test
    void deletesEveryMessageLinkedToStateWithoutChangingOtherMessages() {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var otherState = orgenhetStateService.opprett(sett.id(), stateData(1, STATE_TIME));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 1));
        var otherMessage = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(otherState.id(), 2));
        var standalone = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 3));

        orgenhetStateService.slett(sett.id(), state.id(), true);

        assertThat(orgenhetStateService.finnAlle(sett.id())).containsExactly(otherState);
        assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).containsExactly(otherMessage, standalone);
    }

    @Test
    void parentLockRequiresAnActiveTransaction() {
        var sett = createSet();

        assertThatThrownBy(() -> testSettPessimisticLocking.lockTestSett(sett.id()))
            .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(testSettService.hent(sett.id())).isEqualTo(sett);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bulkStateDeletionKeepsStandaloneMessagesAndOtherSets(boolean deleteMessages) {
        var sett = createSet();
        var first = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var second = orgenhetStateService.opprett(sett.id(), stateData(1, STATE_TIME));
        var linked = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(first.id(), 0));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(second.id(), 1));
        var standalone = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 2));
        var otherSet = createSet();
        var otherState = orgenhetStateService.opprett(otherSet.id(), stateData(0, STATE_TIME));
        var otherMessage = orgenhetEvtsrcMsgService.opprett(otherSet.id(), messageData(otherState.id(), 0));
        setActor("synthetic-editor");

        assertThat(orgenhetStateService.slettAlle(sett.id(), deleteMessages)).isEqualTo(2);

        assertThat(orgenhetStateService.finnAlle(sett.id())).isEmpty();
        assertThat(orgenhetEvtsrcMsgService.hent(sett.id(), standalone.id())).isEqualTo(standalone);
        if (deleteMessages) {
            assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).containsExactly(standalone);
        } else {
            assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).hasSize(3)
                .allSatisfy(message -> assertThat(message.data().orgenhetStateId()).isNull());
            assertThat(orgenhetEvtsrcMsgService.hent(sett.id(), linked.id()).audit().sistEndretAv()).isEqualTo("synthetic-editor");
        }
        assertThat(orgenhetStateService.finnAlle(otherSet.id())).containsExactly(otherState);
        assertThat(orgenhetEvtsrcMsgService.finnAlle(otherSet.id())).containsExactly(otherMessage);
    }

    @Test
    void canDeleteMessagesWithoutDeletingStatesOrSet() {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var first = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 1));
        orgenhetEvtsrcMsgService.slett(sett.id(), first.id());

        assertThat(orgenhetEvtsrcMsgService.slettAlle(sett.id())).isEqualTo(1);
        assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).isEmpty();
        assertThat(orgenhetStateService.finnAlle(sett.id())).containsExactly(state);
        assertThat(testSettService.hent(sett.id())).isEqualTo(sett);
    }

    @Test
    void deletesCompleteSetWithAllChildrenAndLeavesOtherSetUntouched() {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 1));
        var otherSet = createSet();
        var otherState = orgenhetStateService.opprett(otherSet.id(), stateData(0, STATE_TIME));
        var otherMessage = orgenhetEvtsrcMsgService.opprett(otherSet.id(), messageData(otherState.id(), 0));

        testSettService.slett(sett.id());

        assertThatThrownBy(() -> testSettService.hent(sett.id())).isInstanceOf(NoSuchElementException.class);
        assertThat(jdbcClient.sql(
            "SELECT count(*) FROM orgenhet_state WHERE test_sett_id = :testSettId"
        ).param("testSettId", sett.id()).query(Long.class).single()).isZero();
        assertThat(jdbcClient.sql(
            "SELECT count(*) FROM orgenhet_evtsrc_msg WHERE test_sett_id = :testSettId"
        ).param("testSettId", sett.id()).query(Long.class).single()).isZero();
        var other = testSettService.hentKomplett(otherSet.id());
        assertThat(other.states()).containsExactly(otherState);
        assertThat(other.meldinger()).containsExactly(otherMessage);
    }

    @Test
    void swapsStateOrderWithoutUniqueConstraintViolation() {
        var sett = createSet();
        var first = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var second = orgenhetStateService.opprett(sett.id(), stateData(1, STATE_TIME.plusDays(1)));
        setActor("synthetic-editor");

        orgenhetStateService.endreRekkefolge(sett.id(), List.of(second.id(), first.id()));

        var states = orgenhetStateService.finnAlle(sett.id());
        assertThat(states).extracting(OrgenhetState::id).containsExactly(second.id(), first.id());
        assertThat(states).extracting(row -> row.data().sortIdx()).containsExactly(0L, 1L);
        assertThat(states).allSatisfy(row -> assertThat(row.audit().sistEndretAv()).isEqualTo("synthetic-editor"));
        assertThat(orgenhetStateService.hent(sett.id(), first.id()).audit().opprettet()).isEqualTo(first.audit().opprettet());
        assertThat(orgenhetStateService.hent(sett.id(), first.id()).data().stateTimestamp()).isEqualTo(STATE_TIME);
    }

    @Test
    void swapsMessageOrderWithoutUniqueConstraintViolation() {
        var sett = createSet();
        var first = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 0));
        var second = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 1));
        setActor("synthetic-editor");

        orgenhetEvtsrcMsgService.endreRekkefolge(sett.id(), List.of(second.id(), first.id()));

        var messages = orgenhetEvtsrcMsgService.finnAlle(sett.id());
        assertThat(messages).extracting(OrgenhetEvtsrcMsg::id).containsExactly(second.id(), first.id());
        assertThat(messages).extracting(row -> row.data().sortIdx()).containsExactly(0L, 1L);
        assertThat(messages).allSatisfy(row -> assertThat(row.audit().sistEndretAv()).isEqualTo("synthetic-editor"));
    }

    @Test
    void rejectsIncompleteDuplicateNullAndForeignStateOrderWithoutChanges() {
        var sett = createSet();
        var first = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var second = orgenhetStateService.opprett(sett.id(), stateData(1, STATE_TIME));
        var otherSet = createSet();
        var foreign = orgenhetStateService.opprett(otherSet.id(), stateData(0, STATE_TIME));
        List<List<Long>> invalidOrders = Arrays.asList(
            null, List.of(), List.of(first.id()),
            List.of(first.id(), first.id()), List.of(first.id(), foreign.id()), Arrays.asList(first.id(), null)
        );

        for (var order : invalidOrders) {
            assertThatThrownBy(() -> orgenhetStateService.endreRekkefolge(sett.id(), order)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(orgenhetStateService.finnAlle(sett.id())).containsExactly(first, second);
        assertThat(orgenhetStateService.hent(otherSet.id(), foreign.id())).isEqualTo(foreign);
    }

    @Test
    void rejectsIncompleteDuplicateNullAndForeignMessageOrderWithoutChanges() {
        var sett = createSet();
        var first = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 0));
        var second = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 1));
        var otherSet = createSet();
        var foreign = orgenhetEvtsrcMsgService.opprett(otherSet.id(), messageData(null, 0));
        List<List<Long>> invalidOrders = Arrays.asList(
            null, List.of(), List.of(first.id()),
            List.of(first.id(), first.id()), List.of(first.id(), foreign.id()), Arrays.asList(first.id(), null)
        );

        for (var order : invalidOrders) {
            assertThatThrownBy(() -> orgenhetEvtsrcMsgService.endreRekkefolge(sett.id(), order)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).containsExactly(first, second);
        assertThat(orgenhetEvtsrcMsgService.hent(otherSet.id(), foreign.id())).isEqualTo(foreign);
    }

    @Test
    void fullSetDeletionRollsBackTogetherWithItsChildren() {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));
        var before = testSettService.hentKomplett(sett.id());

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            testSettService.slett(sett.id());
            throw new IllegalStateException("synthetic rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(testSettService.hentKomplett(sett.id())).isEqualTo(before);
    }

    @Test
    void detachAndStateDeletionRollBackTogether() {
        var sett = createSet();
        var state = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(state.id(), 0));
        var before = testSettService.hentKomplett(sett.id());

        setActor("synthetic-editor");

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            orgenhetStateService.slett(sett.id(), state.id(), false);
            throw new IllegalStateException("synthetic rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(testSettService.hentKomplett(sett.id())).isEqualTo(before);
    }

    @Test
    void databasePreventsDeletingParentWithRemainingChildren() {
        var sett = createSet();
        orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        assertThatThrownBy(() -> testSettRepository.delete(sett.id())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(testSettService.hent(sett.id())).isEqualTo(sett);
    }

    @ParameterizedTest
    @ValueSource(strings = {"opprettState", "opprettMelding", "endreStateRekkefolge", "endreMeldingRekkefolge"})
    void mutationsWaitForParentLock(String operation) throws Exception {
        var sett = createSet();
        var firstState = orgenhetStateService.opprett(sett.id(), stateData(0, STATE_TIME));
        var secondState = orgenhetStateService.opprett(sett.id(), stateData(1, STATE_TIME));
        var firstMessage = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 0));
        var secondMessage = orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 1));
        var started = new CountDownLatch(1);
        Runnable mutation = switch (operation) {
            case "opprettState" -> () -> orgenhetStateService.opprett(sett.id(), stateData(2, STATE_TIME));
            case "opprettMelding" -> () -> orgenhetEvtsrcMsgService.opprett(sett.id(), messageData(null, 2));
            case "endreStateRekkefolge" -> () -> orgenhetStateService.endreRekkefolge(sett.id(), List.of(secondState.id(), firstState.id()));
            case "endreMeldingRekkefolge" -> () -> orgenhetEvtsrcMsgService.endreRekkefolge(sett.id(), List.of(secondMessage.id(), firstMessage.id()));
            default -> throw new IllegalArgumentException("Unknown test operation");
        };

        try (var executor = Executors.newSingleThreadExecutor()) {
            var pending = new TransactionTemplate(transactionManager).execute(status -> {
                testSettPessimisticLocking.lockTestSett(sett.id());
                var future = executor.submit(() -> {
                    started.countDown();
                    mutation.run();
                });
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for test mutation", e);
                }
                assertThatThrownBy(() -> future.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                return future;
            });
            assertThat(pending).isNotNull();
            pending.get(5, TimeUnit.SECONDS);
        }

        switch (operation) {
            case "opprettState" -> assertThat(orgenhetStateService.finnAlle(sett.id())).hasSize(3);
            case "opprettMelding" -> assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).hasSize(3);
            case "endreStateRekkefolge" -> assertThat(orgenhetStateService.finnAlle(sett.id())).extracting(OrgenhetState::id)
                .containsExactly(secondState.id(), firstState.id());
            case "endreMeldingRekkefolge" -> assertThat(orgenhetEvtsrcMsgService.finnAlle(sett.id())).extracting(OrgenhetEvtsrcMsg::id)
                .containsExactly(secondMessage.id(), firstMessage.id());
        }
    }

    private static OrgenhetState.Data stateData(long index, LocalDateTime timestamp) {
        return new OrgenhetState.Data(
            index, timestamp, "synthetic-fid", "synthetic-unit", "Synthetic unit",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), OrgEnhetsType.values()[0], NomNivaa.values()[0],
            "synthetic-unique", "synthetic-agresso", "synthetic-level", "synthetic-type", "synthetic-access", "synthetic-cost"
        );
    }

    private static OrgenhetEvtsrcMsg.Data messageData(Long stateId, long index) {
        return new OrgenhetEvtsrcMsg.Data(stateId, index, STATE_TIME, "synthetic.Event", "synthetic-key", "  synthetic message\n ");
    }

}
