package no.nav.nom.apps.testdata;

import no.nav.nom.apps.testdata.model.OrgtilknytningEvtsrcMsg;
import no.nav.nom.apps.testdata.model.OrgtilknytningState;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrgtilknytningTest extends RepositoryDomainTestBase {
    private static final LocalDateTime STATE_TIME = LocalDateTime.of(2026, 4, 12, 14, 45, 10);

    @Test
    void fullFieldCrudRoundTripPreservesCreationAudit() {
        var set = createSet();
        var createdData = orgtilknytningData(3, "synthetic-first", List.of("synthetic-office-a", "synthetic-office-b"));
        var created = orgtilknytningStateService.opprett(set.id(), createdData);

        assertThat(created.data()).isEqualTo(createdData);
        assertThat(orgtilknytningStateService.hent(set.id(), created.id())).isEqualTo(created);
        assertThat(orgtilknytningStateService.finnForFid(set.id(), "synthetic-org-fid-shared")).containsExactly(created);

        setActor("synthetic-editor");
        var updatedData = orgtilknytningData(4, "synthetic-updated", List.of("synthetic-office-updated"));
        var updated = orgtilknytningStateService.endre(set.id(), created.id(), updatedData);

        assertThat(updated.data()).isEqualTo(updatedData);
        assertThat(updated.audit().opprettet()).isEqualTo(created.audit().opprettet());
        assertThat(updated.audit().opprettetAv()).isEqualTo(created.audit().opprettetAv());
        assertThat(updated.audit().sistEndretAv()).isEqualTo("synthetic-editor");
        assertThat(updated.data().stateSistEndretAv()).isEqualTo("synthetic-updated-state-actor");
        assertThat(updated.data().stateSistEndretAv()).isNotEqualTo(updated.audit().sistEndretAv());
    }

    @Test
    void nullableAndEmptyArraysAndOrderedDuplicateEntriesRoundTrip() {
        var set = createSet();
        var nullArray = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(0, "synthetic-null-array", null));
        var emptyArray = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(1, "synthetic-empty-array", List.of()));
        var orderedArray = orgtilknytningStateService.opprett(
            set.id(), orgtilknytningData(
                2, "synthetic-ordered-array",
                Arrays.asList("synthetic-repeat", null, "synthetic-second", "synthetic-repeat")
            )
        );

        assertThat(nullArray.data().additionalFysiskeBrukerstederNummer()).isNull();
        assertThat(emptyArray.data().additionalFysiskeBrukerstederNummer()).isEmpty();
        assertThat(orderedArray.data().additionalFysiskeBrukerstederNummer())
            .containsExactly("synthetic-repeat", null, "synthetic-second", "synthetic-repeat");
    }

    @Test
    void nullableDomainFieldsRoundTripOnCreateAndUpdate() {
        var set = createSet();
        var nullData = new OrgtilknytningState.Data(
            0, STATE_TIME, null, null, null, null, false, null, null, null, null, null, null, null
        );

        var createdNull = orgtilknytningStateService.opprett(set.id(), nullData);
        assertThat(createdNull.data()).isEqualTo(nullData);

        var full = orgtilknytningStateService.endre(
            set.id(), createdNull.id(),
            orgtilknytningData(0, "synthetic-full", List.of("synthetic-office"))
        );
        var updatedToNull = orgtilknytningStateService.endre(set.id(), full.id(), nullData);
        assertThat(updatedToNull.data()).isEqualTo(nullData);
    }

    @Test
    void allowsRepeatedFidAndTimestampButEnforcesSetScopedSortIndex() {
        var set = createSet();
        var otherSet = createSet();
        var first = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(0, "synthetic-first", List.of()));
        var second = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(1, "synthetic-second", List.of()));
        var third = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(2, "synthetic-third", List.of()));
        var sameIndexOtherSet = orgtilknytningStateService.opprett(otherSet.id(), orgtilknytningData(0, "synthetic-other", List.of()));

        assertThat(orgtilknytningStateService.finnForFid(set.id(), "synthetic-org-fid-shared"))
            .containsExactly(first, second, third);
        assertThat(orgtilknytningStateService.finnAlle(set.id())).containsExactly(first, second, third);
        assertThat(first.data().stateTimestamp()).isEqualTo(second.data().stateTimestamp());
        assertThat(orgtilknytningStateService.hent(otherSet.id(), sameIndexOtherSet.id())).isEqualTo(sameIndexOtherSet);
        assertThatThrownBy(() -> orgtilknytningStateService.opprett(set.id(), orgtilknytningData(1, "synthetic-duplicate", List.of())))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(orgtilknytningStateService.finnAlle(set.id())).containsExactly(first, second, third);
    }

    @Test
    void rawMessageKeepsWhitespaceAndInvalidJsonAndRejectsCrossSetStateReference() {
        var set = createSet();
        var otherSet = createSet();
        var state = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(0, "synthetic-state", List.of()));
        var standalone = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(null, 0, "  { invalid json\n }  "));
        var linked = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(state.id(), 1, "  raw payload\n  "));

        assertThat(standalone.data().orgtilknytningStateId()).isNull();
        assertThat(standalone.data().evtsrcMessage()).isEqualTo("  { invalid json\n }  ");
        assertThat(linked.data().orgtilknytningStateId()).isEqualTo(state.id());
        assertThatThrownBy(() -> orgtilknytningEvtsrcMsgService.opprett(otherSet.id(), messageData(state.id(), 0, "synthetic-cross-set")))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(orgtilknytningEvtsrcMsgService.finnAlle(set.id())).containsExactly(standalone, linked);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletingOneStateAppliesMessagePolicyAndKeepsUnlinkedMessages(boolean deleteLinkedMessages) {
        var set = createSet();
        var state = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(0, "synthetic-state", List.of()));
        var otherState = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(1, "synthetic-other-state", List.of()));
        var linked = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(state.id(), 0, "synthetic-linked"));
        var unrelated = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(otherState.id(), 1, "synthetic-other"));
        var standalone = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(null, 2, "synthetic-standalone"));
        setActor("synthetic-delete-actor");

        orgtilknytningStateService.slett(set.id(), state.id(), deleteLinkedMessages);

        assertThat(orgtilknytningStateService.finnAlle(set.id())).containsExactly(otherState);
        assertThat(orgtilknytningEvtsrcMsgService.hent(set.id(), unrelated.id())).isEqualTo(unrelated);
        assertThat(orgtilknytningEvtsrcMsgService.hent(set.id(), standalone.id())).isEqualTo(standalone);
        if (deleteLinkedMessages) {
            assertThatThrownBy(() -> orgtilknytningEvtsrcMsgService.hent(set.id(), linked.id()))
                .isInstanceOf(NoSuchElementException.class);
        } else {
            var detached = orgtilknytningEvtsrcMsgService.hent(set.id(), linked.id());
            assertThat(detached.data().orgtilknytningStateId()).isNull();
            assertThat(detached.data().evtsrcMessage()).isEqualTo(linked.data().evtsrcMessage());
            assertThat(detached.audit().opprettet()).isEqualTo(linked.audit().opprettet());
            assertThat(detached.audit().opprettetAv()).isEqualTo(linked.audit().opprettetAv());
            assertThat(detached.audit().sistEndretAv()).isEqualTo("synthetic-delete-actor");
        }

        var emptyState = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(2, "synthetic-empty-state", List.of()));
        orgtilknytningStateService.slett(set.id(), emptyState.id(), deleteLinkedMessages);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bulkDeleteAppliesMessagePolicyAndLeavesOtherSetUntouched(boolean deleteLinkedMessages) {
        var set = createSet();
        var first = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(0, "synthetic-first-state", List.of()));
        var second = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(1, "synthetic-second-state", List.of()));
        var linked = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(first.id(), 0, "synthetic-linked"));
        var standalone = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(null, 1, "synthetic-standalone"));
        var otherSet = createSet();
        var foreignState = orgtilknytningStateService.opprett(otherSet.id(), orgtilknytningData(0, "synthetic-foreign", List.of()));
        var foreignMessage = orgtilknytningEvtsrcMsgService.opprett(otherSet.id(), messageData(foreignState.id(), 0, "synthetic-foreign-message"));
        setActor("synthetic-bulk-actor");

        assertThat(orgtilknytningStateService.slettAlle(set.id(), deleteLinkedMessages)).isEqualTo(2);

        assertThat(orgtilknytningStateService.finnAlle(set.id())).isEmpty();
        assertThat(orgtilknytningEvtsrcMsgService.hent(set.id(), standalone.id())).isEqualTo(standalone);
        if (deleteLinkedMessages) {
            assertThat(orgtilknytningEvtsrcMsgService.finnAlle(set.id())).containsExactly(standalone);
        } else {
            var detached = orgtilknytningEvtsrcMsgService.hent(set.id(), linked.id());
            assertThat(orgtilknytningEvtsrcMsgService.finnAlle(set.id())).hasSize(2)
                .allSatisfy(message -> assertThat(message.data().orgtilknytningStateId()).isNull());
            assertThat(detached.audit().sistEndretAv()).isEqualTo("synthetic-bulk-actor");
        }
        assertThat(orgtilknytningStateService.finnAlle(otherSet.id())).containsExactly(foreignState);
        assertThat(orgtilknytningEvtsrcMsgService.finnAlle(otherSet.id())).containsExactly(foreignMessage);
    }

    @Test
    void reorderSwapsRowsAndInvalidOrdersAndWrongSetOperationsDoNotChangeParents() {
        var set = createSet();
        var otherSet = createSet();
        var first = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(0, "synthetic-order-first", List.of()));
        var second = orgtilknytningStateService.opprett(set.id(), orgtilknytningData(1, "synthetic-order-second", List.of()));
        var foreign = orgtilknytningStateService.opprett(otherSet.id(), orgtilknytningData(0, "synthetic-order-foreign", List.of()));
        var messageFirst = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(null, 0, "synthetic-message-first"));
        var messageSecond = orgtilknytningEvtsrcMsgService.opprett(set.id(), messageData(null, 1, "synthetic-message-second"));
        var foreignMessage = orgtilknytningEvtsrcMsgService.opprett(otherSet.id(), messageData(null, 0, "synthetic-message-foreign"));

        orgtilknytningStateService.endreRekkefolge(set.id(), List.of(second.id(), first.id()));
        orgtilknytningEvtsrcMsgService.endreRekkefolge(set.id(), List.of(messageSecond.id(), messageFirst.id()));
        assertThat(orgtilknytningStateService.finnAlle(set.id())).extracting(OrgtilknytningState::id)
            .containsExactly(second.id(), first.id());
        assertThat(orgtilknytningStateService.finnAlle(set.id())).extracting(row -> row.data().sortIdx()).containsExactly(0L, 1L);
        assertThat(orgtilknytningEvtsrcMsgService.finnAlle(set.id())).extracting(OrgtilknytningEvtsrcMsg::id)
            .containsExactly(messageSecond.id(), messageFirst.id());

        assertThatThrownBy(() -> orgtilknytningStateService.endreRekkefolge(set.id(), List.of(first.id(), first.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orgtilknytningStateService.endreRekkefolge(set.id(), List.of(first.id(), foreign.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orgtilknytningEvtsrcMsgService.endreRekkefolge(set.id(), List.of(messageFirst.id(), messageFirst.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orgtilknytningEvtsrcMsgService.endreRekkefolge(set.id(), List.of(messageFirst.id(), foreignMessage.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orgtilknytningStateService.hent(otherSet.id(), first.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgtilknytningStateService.endre(otherSet.id(), first.id(), orgtilknytningData(4, "synthetic-wrong-set", List.of())))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgtilknytningStateService.slett(otherSet.id(), first.id(), true)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgtilknytningEvtsrcMsgService.hent(otherSet.id(), messageFirst.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgtilknytningEvtsrcMsgService.endre(otherSet.id(), messageFirst.id(), messageData(null, 3, "synthetic-wrong-set")))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> orgtilknytningEvtsrcMsgService.slett(otherSet.id(), messageFirst.id())).isInstanceOf(NoSuchElementException.class);

        assertThat(orgtilknytningStateService.finnAlle(set.id())).extracting(OrgtilknytningState::id)
            .containsExactly(second.id(), first.id());
        assertThat(orgtilknytningEvtsrcMsgService.finnAlle(set.id())).extracting(OrgtilknytningEvtsrcMsg::id)
            .containsExactly(messageSecond.id(), messageFirst.id());
        assertThat(orgtilknytningStateService.hent(otherSet.id(), foreign.id())).isEqualTo(foreign);
        assertThat(orgtilknytningEvtsrcMsgService.hent(otherSet.id(), foreignMessage.id())).isEqualTo(foreignMessage);
    }

    private static OrgtilknytningState.Data orgtilknytningData(long sortIdx, String suffix, List<String> physicalLocations) {
        return new OrgtilknytningState.Data(
            sortIdx, STATE_TIME, "synthetic-org-fid-shared", "synthetic-org-navident", "synthetic-org-unit", Sektor.EKSTERN,
            true, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), Tilknytningtype.MIDLERTIDIG,
            "synthetic-primary-office", physicalLocations, "synthetic-moved-from-" + suffix, "synthetic-updated-state-actor"
        );
    }

    private static OrgtilknytningEvtsrcMsg.Data messageData(Long stateId, long sortIdx, String message) {
        return new OrgtilknytningEvtsrcMsg.Data(stateId, sortIdx, STATE_TIME, "synthetic.OrgtilknytningEvent", "synthetic-message-key", message);
    }
}
