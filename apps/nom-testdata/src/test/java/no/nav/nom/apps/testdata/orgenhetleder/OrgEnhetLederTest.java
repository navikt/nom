package no.nav.nom.apps.testdata.orgenhetleder;

import no.nav.nom.apps.testdata.RepositoryDomainTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrgEnhetLederTest extends RepositoryDomainTestBase {
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 3, 11, 12, 13);

    @Autowired
    private OrgEnhetLederStateService stateService;
    @Autowired
    private OrgEnhetLederEvtsrcMsgService meldingService;

    @Test
    void stateCrudRoundTripsAllFieldsIncludingNullableDatesAndSeparatesDomainEditorFromAudit() {
        var sett = createSet();
        var originalData = stateData(
            0, TIMESTAMP, "synthetic-fid-a", "synthetic-navident-a", "synthetic-unit-a",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "synthetic-domain-editor"
        );
        var created = stateService.opprett(sett.id(), originalData);

        assertThat(created.data()).isEqualTo(originalData);
        assertThat(stateService.hent(sett.id(), created.id())).isEqualTo(created);
        assertThat(stateService.finnAlle(sett.id())).containsExactly(created);
        assertThat(stateService.finnForFid(sett.id(), "synthetic-fid-a")).containsExactly(created);

        var updatedData = stateData(
            3, TIMESTAMP.plusDays(2), "synthetic-fid-b", "synthetic-navident-b", "synthetic-unit-b",
            null, null, "synthetic-updated-domain-editor"
        );
        var updated = stateService.endre(sett.id(), created.id(), updatedData);

        assertThat(updated.data()).isEqualTo(updatedData);
        assertThat(stateService.hent(sett.id(), created.id())).isEqualTo(updated);
        assertThat(stateService.finnForFid(sett.id(), "synthetic-fid-a")).isEmpty();
        assertThat(stateService.finnForFid(sett.id(), "synthetic-fid-b")).containsExactly(updated);
        assertThat(updated.data().stateSistEndretAv()).isEqualTo("synthetic-updated-domain-editor");
        assertThat(updated.audit().sistEndretAv()).isEqualTo("nom-testdata");
    }

    @Test
    void repeatedFidAndTimestampAreAllowedAndRowsSortOnlyBySortIndex() {
        var sett = createSet();
        var third = stateService.opprett(
            sett.id(), stateData(
                30, TIMESTAMP, "synthetic-fid", "synthetic-navident-3",
                "synthetic-unit-3", null, null, "synthetic-editor"
            )
        );
        var first = stateService.opprett(
            sett.id(), stateData(
                10, TIMESTAMP, "synthetic-fid", "synthetic-navident-1",
                "synthetic-unit-1", null, null, "synthetic-editor"
            )
        );
        var second = stateService.opprett(
            sett.id(), stateData(
                20, TIMESTAMP, "synthetic-fid", "synthetic-navident-2",
                "synthetic-unit-2", null, null, "synthetic-editor"
            )
        );

        assertThat(stateService.finnAlle(sett.id())).containsExactly(first, second, third);
        assertThat(stateService.finnForFid(sett.id(), "synthetic-fid")).containsExactly(first, second, third);
    }

    @Test
    void messageKeepsRawWhitespaceAndSupportsNullableStateReference() {
        var sett = createSet();
        var state = stateService.opprett(
            sett.id(), stateData(
                0, TIMESTAMP, "synthetic-fid", "synthetic-navident",
                "synthetic-unit", null, null, "synthetic-editor"
            )
        );
        var linked = meldingService.opprett(sett.id(), messageData(state.id(), 0, "  {not-json}\n \t"));
        var standalone = meldingService.opprett(sett.id(), messageData(null, 1, "  standalone payload\n "));

        assertThat(linked.data()).isEqualTo(messageData(state.id(), 0, "  {not-json}\n \t"));
        assertThat(standalone.data()).isEqualTo(messageData(null, 1, "  standalone payload\n "));
        assertThat(meldingService.hent(sett.id(), linked.id())).isEqualTo(linked);
        assertThat(meldingService.finnAlle(sett.id())).containsExactly(linked, standalone);
    }

    @Test
    void rejectsCrossSetStateLinksAndDoesNotExposeRowsThroughWrongParent() {
        var sett = createSet();
        var otherSet = createSet();
        var state = stateService.opprett(
            sett.id(), stateData(
                0, TIMESTAMP, "synthetic-fid", "synthetic-navident",
                "synthetic-unit", null, null, "synthetic-editor"
            )
        );
        var message = meldingService.opprett(otherSet.id(), messageData(null, 0, "synthetic standalone"));

        assertThatThrownBy(() -> meldingService.opprett(otherSet.id(), messageData(state.id(), 1)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> meldingService.endre(otherSet.id(), message.id(), messageData(state.id(), 0)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> stateService.hent(otherSet.id(), state.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> stateService.endre(otherSet.id(), state.id(), state.data())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> stateService.slett(otherSet.id(), state.id(), true)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> meldingService.hent(sett.id(), message.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> meldingService.endre(sett.id(), message.id(), message.data())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> meldingService.slett(sett.id(), message.id())).isInstanceOf(NoSuchElementException.class);

        assertThat(stateService.hent(sett.id(), state.id())).isEqualTo(state);
        assertThat(meldingService.hent(otherSet.id(), message.id())).isEqualTo(message);
        assertThat(meldingService.finnAlle(sett.id())).isEmpty();
        assertThat(meldingService.finnAlle(otherSet.id())).containsExactly(message);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void singleAndBulkStateDeletionApplyMessagePolicyAndKeepOtherSetUntouched(boolean bulk, boolean deleteMessages) {
        var sett = createSet();
        var target = stateService.opprett(
            sett.id(), stateData(
                0, TIMESTAMP, "synthetic-target", "synthetic-navident-target",
                "synthetic-target-unit", null, null, "synthetic-editor"
            )
        );
        var retainedState = stateService.opprett(
            sett.id(), stateData(
                1, TIMESTAMP, "synthetic-retained", "synthetic-navident-retained",
                "synthetic-retained-unit", null, null, "synthetic-editor"
            )
        );
        var linked = meldingService.opprett(sett.id(), messageData(target.id(), 0));
        var otherLinked = meldingService.opprett(sett.id(), messageData(retainedState.id(), 1));
        var standalone = meldingService.opprett(sett.id(), messageData(null, 2));
        var otherSet = createSet();
        var otherState = stateService.opprett(
            otherSet.id(), stateData(
                0, TIMESTAMP, "synthetic-other", "synthetic-navident-other",
                "synthetic-other-unit", null, null, "synthetic-editor"
            )
        );
        var otherMessage = meldingService.opprett(otherSet.id(), messageData(otherState.id(), 0));

        if (bulk) {
            assertThat(stateService.slettAlle(sett.id(), deleteMessages)).isEqualTo(2);
            assertThat(stateService.finnAlle(sett.id())).isEmpty();
            if (deleteMessages) {
                assertThat(meldingService.finnAlle(sett.id())).containsExactly(standalone);
            } else {
                var remaining = meldingService.finnAlle(sett.id());
                assertThat(remaining).hasSize(3).allSatisfy(row -> assertThat(row.data().orgEnhetLederStateId()).isNull());
                assertThat(remaining).extracting(row -> row.data().evtsrcMessage())
                    .containsExactly(linked.data().evtsrcMessage(), otherLinked.data().evtsrcMessage(), standalone.data().evtsrcMessage());
            }
            assertThat(meldingService.hent(sett.id(), standalone.id()).data().orgEnhetLederStateId()).isNull();
        } else {
            stateService.slett(sett.id(), target.id(), deleteMessages);
            assertThat(stateService.finnAlle(sett.id())).containsExactly(retainedState);
            assertThat(meldingService.hent(sett.id(), otherLinked.id())).isEqualTo(otherLinked);
            assertThat(meldingService.hent(sett.id(), standalone.id())).isEqualTo(standalone);
            if (deleteMessages) {
                assertThatThrownBy(() -> meldingService.hent(sett.id(), linked.id())).isInstanceOf(NoSuchElementException.class);
            } else {
                var detached = meldingService.hent(sett.id(), linked.id());
                assertThat(detached.data().orgEnhetLederStateId()).isNull();
                assertThat(detached.data().evtsrcMessage()).isEqualTo(linked.data().evtsrcMessage());
            }
        }
        assertThat(stateService.finnAlle(otherSet.id())).containsExactly(otherState);
        assertThat(meldingService.finnAlle(otherSet.id())).containsExactly(otherMessage);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void deletingStateWithoutMessagesWorksForEitherPolicy(boolean bulk, boolean deleteMessages) {
        var sett = createSet();
        var state = stateService.opprett(
            sett.id(), stateData(
                0, TIMESTAMP, "synthetic-fid", "synthetic-navident",
                "synthetic-unit", null, null, "synthetic-editor"
            )
        );

        if (bulk) {
            assertThat(stateService.slettAlle(sett.id(), deleteMessages)).isEqualTo(1);
        } else {
            stateService.slett(sett.id(), state.id(), deleteMessages);
        }

        assertThat(stateService.finnAlle(sett.id())).isEmpty();
        assertThat(meldingService.finnAlle(sett.id())).isEmpty();
        assertThat(testSettService.hent(sett.id())).isEqualTo(sett);
    }

    @Test
    void swapsStateAndMessageOrderAndRejectsDuplicateOrForeignIdsWithoutChanges() {
        var sett = createSet();
        var first = stateService.opprett(
            sett.id(), stateData(
                0, TIMESTAMP, "synthetic-a", "synthetic-navident-a", "synthetic-unit-a",
                null, null, "synthetic-editor"
            )
        );
        var second = stateService.opprett(
            sett.id(), stateData(
                1, TIMESTAMP.plusDays(1), "synthetic-b", "synthetic-navident-b",
                "synthetic-unit-b", null, null, "synthetic-editor"
            )
        );
        var firstMessage = meldingService.opprett(sett.id(), messageData(null, 0));
        var secondMessage = meldingService.opprett(sett.id(), messageData(null, 1));
        var otherSet = createSet();
        var foreignState = stateService.opprett(
            otherSet.id(), stateData(
                0, TIMESTAMP, "synthetic-foreign", "synthetic-navident-foreign",
                "synthetic-unit-foreign", null, null, "synthetic-editor"
            )
        );
        var foreignMessage = meldingService.opprett(otherSet.id(), messageData(null, 0));

        stateService.endreRekkefolge(sett.id(), List.of(second.id(), first.id()));
        meldingService.endreRekkefolge(sett.id(), List.of(secondMessage.id(), firstMessage.id()));
        assertThat(stateService.finnAlle(sett.id())).extracting(OrgEnhetLederState::id).containsExactly(second.id(), first.id());
        assertThat(stateService.finnAlle(sett.id())).extracting(row -> row.data().sortIdx()).containsExactly(0L, 1L);
        assertThat(meldingService.finnAlle(sett.id())).extracting(OrgEnhetLederEvtsrcMsg::id)
            .containsExactly(secondMessage.id(), firstMessage.id());
        assertThat(meldingService.finnAlle(sett.id())).extracting(row -> row.data().sortIdx()).containsExactly(0L, 1L);

        assertThatThrownBy(() -> stateService.endreRekkefolge(sett.id(), List.of(first.id(), first.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stateService.endreRekkefolge(sett.id(), List.of(first.id(), foreignState.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> meldingService.endreRekkefolge(sett.id(), List.of(firstMessage.id(), firstMessage.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> meldingService.endreRekkefolge(sett.id(), List.of(firstMessage.id(), foreignMessage.id())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(stateService.finnAlle(sett.id())).extracting(OrgEnhetLederState::id).containsExactly(second.id(), first.id());
        assertThat(meldingService.finnAlle(sett.id())).extracting(OrgEnhetLederEvtsrcMsg::id)
            .containsExactly(secondMessage.id(), firstMessage.id());
        assertThat(stateService.hent(otherSet.id(), foreignState.id())).isEqualTo(foreignState);
        assertThat(meldingService.hent(otherSet.id(), foreignMessage.id())).isEqualTo(foreignMessage);
    }

    private static OrgEnhetLederState.Data stateData(
        long sortIdx, LocalDateTime timestamp, String fid, String navident, String orgEnhetId,
        LocalDate startdato, LocalDate sluttdato, String stateSistEndretAv
    ) {
        return new OrgEnhetLederState.Data(sortIdx, timestamp, fid, navident, orgEnhetId, startdato, sluttdato, stateSistEndretAv);
    }

    private static OrgEnhetLederEvtsrcMsg.Data messageData(Long stateId, long sortIdx) {
        return messageData(stateId, sortIdx, "synthetic message payload");
    }

    private static OrgEnhetLederEvtsrcMsg.Data messageData(Long stateId, long sortIdx, String payload) {
        return new OrgEnhetLederEvtsrcMsg.Data(stateId, sortIdx, TIMESTAMP, "synthetic.Event", "synthetic-key", payload);
    }
}
