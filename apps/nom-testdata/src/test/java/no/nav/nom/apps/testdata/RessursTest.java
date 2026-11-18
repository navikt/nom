package no.nav.nom.apps.testdata;

import no.nav.nom.apps.testdata.model.RessursEvtsrcMsg;
import no.nav.nom.apps.testdata.model.RessursState;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
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

class RessursTest extends RepositoryDomainTestBase {
    private static final LocalDateTime STATE_TIME = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Test
    void fullFieldCrudRoundTripPreservesCreationAudit() {
        var set = createSet();
        var createdData = ressursData(3, "synthetic-first", List.of("synthetic-office-a", "synthetic-office-b"));
        var created = ressursStateService.opprett(set.id(), createdData);

        assertThat(created.data()).isEqualTo(createdData);
        assertThat(ressursStateService.hent(set.id(), created.id())).isEqualTo(created);
        assertThat(ressursStateService.finnForFid(set.id(), "synthetic-fid-shared")).containsExactly(created);

        setActor("synthetic-editor");
        var updatedData = ressursData(4, "synthetic-updated", List.of("synthetic-office-updated"));
        var updated = ressursStateService.endre(set.id(), created.id(), updatedData);

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
        var nullArray = ressursStateService.opprett(set.id(), ressursData(0, "synthetic-null-array", null));
        var emptyArray = ressursStateService.opprett(set.id(), ressursData(1, "synthetic-empty-array", List.of()));
        var orderedArray = ressursStateService.opprett(
            set.id(), ressursData(
                2, "synthetic-ordered-array",
                Arrays.asList("synthetic-repeat", null, "synthetic-second", "synthetic-repeat")
            )
        );

        assertThat(nullArray.data().fysiskeBrukersteder()).isNull();
        assertThat(emptyArray.data().fysiskeBrukersteder()).isEmpty();
        assertThat(orderedArray.data().fysiskeBrukersteder())
            .containsExactly("synthetic-repeat", null, "synthetic-second", "synthetic-repeat");
    }

    @Test
    void nullableDomainFieldsRoundTripOnCreateAndUpdate() {
        var set = createSet();
        var nullData = new RessursState.Data(
            0, STATE_TIME, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null
        );

        var createdNull = ressursStateService.opprett(set.id(), nullData);
        assertThat(createdNull.data()).isEqualTo(nullData);

        var full = ressursStateService.endre(set.id(), createdNull.id(), ressursData(0, "synthetic-full", List.of("synthetic-office")));
        var updatedToNull = ressursStateService.endre(set.id(), full.id(), nullData);
        assertThat(updatedToNull.data()).isEqualTo(nullData);
    }

    @Test
    void allowsRepeatedFidAndTimestampButEnforcesSetScopedSortIndex() {
        var set = createSet();
        var otherSet = createSet();
        var first = ressursStateService.opprett(set.id(), ressursData(0, "synthetic-first", List.of()));
        var second = ressursStateService.opprett(set.id(), ressursData(1, "synthetic-second", List.of()));
        var third = ressursStateService.opprett(set.id(), ressursData(2, "synthetic-third", List.of()));
        var sameIndexOtherSet = ressursStateService.opprett(otherSet.id(), ressursData(0, "synthetic-other", List.of()));

        assertThat(ressursStateService.finnForFid(set.id(), "synthetic-fid-shared"))
            .containsExactly(first, second, third);
        assertThat(ressursStateService.finnAlle(set.id())).containsExactly(first, second, third);
        assertThat(first.data().stateTimestamp()).isEqualTo(second.data().stateTimestamp());
        assertThat(ressursStateService.hent(otherSet.id(), sameIndexOtherSet.id())).isEqualTo(sameIndexOtherSet);
        assertThatThrownBy(() -> ressursStateService.opprett(set.id(), ressursData(1, "synthetic-duplicate", List.of())))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(ressursStateService.finnAlle(set.id())).containsExactly(first, second, third);
    }

    @Test
    void rawMessageKeepsWhitespaceAndInvalidJsonAndRejectsCrossSetStateReference() {
        var set = createSet();
        var otherSet = createSet();
        var state = ressursStateService.opprett(set.id(), ressursData(0, "synthetic-state", List.of()));
        var standalone = ressursEvtsrcMsgService.opprett(set.id(), messageData(null, 0, "  { invalid json\n }  "));
        var linked = ressursEvtsrcMsgService.opprett(set.id(), messageData(state.id(), 1, "  raw payload\n  "));

        assertThat(standalone.data().ressursStateId()).isNull();
        assertThat(standalone.data().evtsrcMessage()).isEqualTo("  { invalid json\n }  ");
        assertThat(linked.data().ressursStateId()).isEqualTo(state.id());
        assertThatThrownBy(() -> ressursEvtsrcMsgService.opprett(otherSet.id(), messageData(state.id(), 0, "synthetic-cross-set")))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(ressursEvtsrcMsgService.finnAlle(set.id())).containsExactly(standalone, linked);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletingOneStateAppliesMessagePolicyAndKeepsUnlinkedMessages(boolean deleteLinkedMessages) {
        var set = createSet();
        var state = ressursStateService.opprett(set.id(), ressursData(0, "synthetic-state", List.of()));
        var otherState = ressursStateService.opprett(set.id(), ressursData(1, "synthetic-other-state", List.of()));
        var linked = ressursEvtsrcMsgService.opprett(set.id(), messageData(state.id(), 0, "synthetic-linked"));
        var unrelated = ressursEvtsrcMsgService.opprett(set.id(), messageData(otherState.id(), 1, "synthetic-other"));
        var standalone = ressursEvtsrcMsgService.opprett(set.id(), messageData(null, 2, "synthetic-standalone"));
        setActor("synthetic-delete-actor");

        ressursStateService.slett(set.id(), state.id(), deleteLinkedMessages);

        assertThat(ressursStateService.finnAlle(set.id())).containsExactly(otherState);
        assertThat(ressursEvtsrcMsgService.hent(set.id(), unrelated.id())).isEqualTo(unrelated);
        assertThat(ressursEvtsrcMsgService.hent(set.id(), standalone.id())).isEqualTo(standalone);
        if (deleteLinkedMessages) {
            assertThatThrownBy(() -> ressursEvtsrcMsgService.hent(set.id(), linked.id()))
                .isInstanceOf(NoSuchElementException.class);
        } else {
            var detached = ressursEvtsrcMsgService.hent(set.id(), linked.id());
            assertThat(detached.data().ressursStateId()).isNull();
            assertThat(detached.data().evtsrcMessage()).isEqualTo(linked.data().evtsrcMessage());
            assertThat(detached.audit().opprettet()).isEqualTo(linked.audit().opprettet());
            assertThat(detached.audit().opprettetAv()).isEqualTo(linked.audit().opprettetAv());
            assertThat(detached.audit().sistEndretAv()).isEqualTo("synthetic-delete-actor");
        }

        var emptyState = ressursStateService.opprett(set.id(), ressursData(2, "synthetic-empty-state", List.of()));
        ressursStateService.slett(set.id(), emptyState.id(), deleteLinkedMessages);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bulkDeleteAppliesMessagePolicyAndLeavesOtherSetUntouched(boolean deleteLinkedMessages) {
        var set = createSet();
        var first = ressursStateService.opprett(set.id(), ressursData(0, "synthetic-first-state", List.of()));
        var second = ressursStateService.opprett(set.id(), ressursData(1, "synthetic-second-state", List.of()));
        var linked = ressursEvtsrcMsgService.opprett(set.id(), messageData(first.id(), 0, "synthetic-linked"));
        var standalone = ressursEvtsrcMsgService.opprett(set.id(), messageData(null, 1, "synthetic-standalone"));
        var otherSet = createSet();
        var foreignState = ressursStateService.opprett(otherSet.id(), ressursData(0, "synthetic-foreign", List.of()));
        var foreignMessage = ressursEvtsrcMsgService.opprett(otherSet.id(), messageData(foreignState.id(), 0, "synthetic-foreign-message"));
        setActor("synthetic-bulk-actor");

        assertThat(ressursStateService.slettAlle(set.id(), deleteLinkedMessages)).isEqualTo(2);

        assertThat(ressursStateService.finnAlle(set.id())).isEmpty();
        assertThat(ressursEvtsrcMsgService.hent(set.id(), standalone.id())).isEqualTo(standalone);
        if (deleteLinkedMessages) {
            assertThat(ressursEvtsrcMsgService.finnAlle(set.id())).containsExactly(standalone);
        } else {
            var detached = ressursEvtsrcMsgService.hent(set.id(), linked.id());
            assertThat(ressursEvtsrcMsgService.finnAlle(set.id())).hasSize(2)
                .allSatisfy(message -> assertThat(message.data().ressursStateId()).isNull());
            assertThat(detached.audit().sistEndretAv()).isEqualTo("synthetic-bulk-actor");
        }
        assertThat(ressursStateService.finnAlle(otherSet.id())).containsExactly(foreignState);
        assertThat(ressursEvtsrcMsgService.finnAlle(otherSet.id())).containsExactly(foreignMessage);
    }

    @Test
    void reorderSwapsRowsAndInvalidOrdersAndWrongSetOperationsDoNotChangeParents() {
        var set = createSet();
        var otherSet = createSet();
        var first = ressursStateService.opprett(set.id(), ressursData(0, "synthetic-order-first", List.of()));
        var second = ressursStateService.opprett(set.id(), ressursData(1, "synthetic-order-second", List.of()));
        var foreign = ressursStateService.opprett(otherSet.id(), ressursData(0, "synthetic-order-foreign", List.of()));
        var messageFirst = ressursEvtsrcMsgService.opprett(set.id(), messageData(null, 0, "synthetic-message-first"));
        var messageSecond = ressursEvtsrcMsgService.opprett(set.id(), messageData(null, 1, "synthetic-message-second"));
        var foreignMessage = ressursEvtsrcMsgService.opprett(otherSet.id(), messageData(null, 0, "synthetic-message-foreign"));

        ressursStateService.endreRekkefolge(set.id(), List.of(second.id(), first.id()));
        ressursEvtsrcMsgService.endreRekkefolge(set.id(), List.of(messageSecond.id(), messageFirst.id()));
        assertThat(ressursStateService.finnAlle(set.id())).extracting(RessursState::id).containsExactly(second.id(), first.id());
        assertThat(ressursStateService.finnAlle(set.id())).extracting(row -> row.data().sortIdx()).containsExactly(0L, 1L);
        assertThat(ressursEvtsrcMsgService.finnAlle(set.id())).extracting(RessursEvtsrcMsg::id)
            .containsExactly(messageSecond.id(), messageFirst.id());

        assertThatThrownBy(() -> ressursStateService.endreRekkefolge(set.id(), List.of(first.id(), first.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ressursStateService.endreRekkefolge(set.id(), List.of(first.id(), foreign.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ressursEvtsrcMsgService.endreRekkefolge(set.id(), List.of(messageFirst.id(), messageFirst.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ressursEvtsrcMsgService.endreRekkefolge(set.id(), List.of(messageFirst.id(), foreignMessage.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ressursStateService.hent(otherSet.id(), first.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> ressursStateService.endre(otherSet.id(), first.id(), ressursData(4, "synthetic-wrong-set", List.of())))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> ressursStateService.slett(otherSet.id(), first.id(), true)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> ressursEvtsrcMsgService.hent(otherSet.id(), messageFirst.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> ressursEvtsrcMsgService.endre(otherSet.id(), messageFirst.id(), messageData(null, 3, "synthetic-wrong-set")))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> ressursEvtsrcMsgService.slett(otherSet.id(), messageFirst.id())).isInstanceOf(NoSuchElementException.class);

        assertThat(ressursStateService.finnAlle(set.id())).extracting(RessursState::id).containsExactly(second.id(), first.id());
        assertThat(ressursEvtsrcMsgService.finnAlle(set.id())).extracting(RessursEvtsrcMsg::id)
            .containsExactly(messageSecond.id(), messageFirst.id());
        assertThat(ressursStateService.hent(otherSet.id(), foreign.id())).isEqualTo(foreign);
        assertThat(ressursEvtsrcMsgService.hent(otherSet.id(), foreignMessage.id())).isEqualTo(foreignMessage);
    }

    private static RessursState.Data ressursData(long sortIdx, String suffix, List<String> physicalLocations) {
        return new RessursState.Data(
            sortIdx, STATE_TIME, "synthetic-fid-shared", "synthetic-personident", "synthetic-navident", Sektor.NAV_STATLIG,
            "synthetic-first-name-" + suffix, "synthetic-middle-name-" + suffix, "synthetic-last-name-" + suffix,
            "synthetic-job-phone", TelefonType.NAV_TJENESTE_TELEFON, "synthetic-primary-phone",
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 1, 1), "synthetic-primary-office", physicalLocations,
            IdentType.STANDARD, "synthetic-updated-state-actor", Tilknytningtype.FAST
        );
    }

    private static RessursEvtsrcMsg.Data messageData(Long stateId, long sortIdx, String message) {
        return new RessursEvtsrcMsg.Data(stateId, sortIdx, STATE_TIME, "synthetic.RessursEvent", "synthetic-message-key", message);
    }
}
