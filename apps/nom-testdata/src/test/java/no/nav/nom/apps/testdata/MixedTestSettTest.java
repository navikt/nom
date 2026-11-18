package no.nav.nom.apps.testdata;

import no.nav.nom.apps.testdata.model.KomplettTestSett;
import no.nav.nom.apps.testdata.model.OrgEnhetLederEvtsrcMsg;
import no.nav.nom.apps.testdata.model.OrgEnhetLederState;
import no.nav.nom.apps.testdata.model.OrganiseringEvtsrcMsg;
import no.nav.nom.apps.testdata.model.OrganiseringState;
import no.nav.nom.apps.testdata.model.OrgenhetEvtsrcMsg;
import no.nav.nom.apps.testdata.model.OrgenhetState;
import no.nav.nom.apps.testdata.model.OrgtilknytningEvtsrcMsg;
import no.nav.nom.apps.testdata.model.OrgtilknytningState;
import no.nav.nom.apps.testdata.model.RessursEvtsrcMsg;
import no.nav.nom.apps.testdata.model.RessursState;
import no.nav.nom.apps.testdata.model.TestSett;
import no.nav.nom.apps.testdata.service.TestSettPessimisticLocking;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.Tilknytningtype;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MixedTestSettTest extends RepositoryDomainTestBase {
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 4, 12, 13, 14);

    @Autowired
    private TestSettPessimisticLocking testSettPessimisticLocking;

    @Test
    void completeSetReturnsAllElevenCollectionsScopedAndSorted() {
        var firstSet = createNamedSet("synthetic-private-parent-name");
        var firstRows = createRows(firstSet.id(), "first");
        var secondSet = createNamedSet("synthetic-other-parent-name");
        createRows(secondSet.id(), "second");

        var complete = testSettService.hentKomplett(firstSet.id());

        assertThat(complete.testSett()).isEqualTo(firstSet);
        assertThat(complete.testSett().id()).isEqualTo(firstSet.id());
        assertThat(complete.states()).containsExactlyElementsOf(ordered(firstRows.orgenhetStates(), OrgenhetState::data));
        assertThat(complete.meldinger()).containsExactlyElementsOf(ordered(firstRows.orgenhetMeldinger(), OrgenhetEvtsrcMsg::data));
        assertThat(complete.ressursStates()).containsExactlyElementsOf(ordered(firstRows.ressursStates(), RessursState::data));
        assertThat(complete.ressursMeldinger()).containsExactlyElementsOf(ordered(firstRows.ressursMeldinger(), RessursEvtsrcMsg::data));
        assertThat(complete.organiseringStates()).containsExactlyElementsOf(ordered(firstRows.organiseringStates(), OrganiseringState::data));
        assertThat(complete.organiseringMeldinger()).containsExactlyElementsOf(ordered(firstRows.organiseringMeldinger(), OrganiseringEvtsrcMsg::data));
        assertThat(complete.orgenhetLederStates()).containsExactlyElementsOf(ordered(firstRows.orgEnhetLederStates(), OrgEnhetLederState::data));
        assertThat(complete.orgenhetLederMeldinger()).containsExactlyElementsOf(ordered(firstRows.orgEnhetLederMeldinger(), OrgEnhetLederEvtsrcMsg::data));
        assertThat(complete.orgtilknytningStates()).containsExactlyElementsOf(ordered(firstRows.orgtilknytningStates(), OrgtilknytningState::data));
        assertThat(complete.orgtilknytningMeldinger()).containsExactlyElementsOf(ordered(firstRows.orgtilknytningMeldinger(), OrgtilknytningEvtsrcMsg::data));
        assertEveryCollectionBelongsTo(firstSet.id(), complete);
    }

    @Test
    void deletingParentRemovesAllFiveStateMessagePairsAndLeavesOtherParentUntouched() {
        var deletedSet = createNamedSet("synthetic-deleted-parent");
        createRows(deletedSet.id(), "deleted");
        var remainingSet = createNamedSet("synthetic-remaining-parent");
        var remainingRows = createRows(remainingSet.id(), "remaining");
        var before = testSettService.hentKomplett(remainingSet.id());

        testSettService.slett(deletedSet.id());

        assertThatThrownBy(() -> testSettService.hent(deletedSet.id())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> testSettService.hentKomplett(deletedSet.id())).isInstanceOf(NoSuchElementException.class);
        assertChildTableEmpty("orgenhet_state", deletedSet.id());
        assertChildTableEmpty("orgenhet_evtsrc_msg", deletedSet.id());
        assertChildTableEmpty("ressurs_state", deletedSet.id());
        assertChildTableEmpty("ressurs_evtsrc_msg", deletedSet.id());
        assertChildTableEmpty("organisering_state", deletedSet.id());
        assertChildTableEmpty("organisering_evtsrc_msg", deletedSet.id());
        assertChildTableEmpty("orgenhet_leder_state", deletedSet.id());
        assertChildTableEmpty("orgenhet_leder_evtsrc_msg", deletedSet.id());
        assertChildTableEmpty("orgtilknytning_state", deletedSet.id());
        assertChildTableEmpty("orgtilknytning_evtsrc_msg", deletedSet.id());
        assertThat(testSettService.hentKomplett(remainingSet.id())).isEqualTo(before);
        assertThat(before.organiseringStates()).containsExactlyElementsOf(ordered(remainingRows.organiseringStates(), OrganiseringState::data));
    }

    @Test
    void deletingParentRollsBackAllDomainsTogether() {
        var sett = createNamedSet("synthetic-rollback-parent");
        createRows(sett.id(), "rollback");
        var before = testSettService.hentKomplett(sett.id());

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            testSettService.slett(sett.id());
            throw new IllegalStateException("synthetic rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("synthetic rollback");

        assertThat(testSettService.hentKomplett(sett.id())).isEqualTo(before);
    }

    @Test
    void newDomainMutationWaitsForParentLockUntilOwningTransactionCommits() throws Exception {
        var sett = createNamedSet("synthetic-lock-parent");
        var started = new CountDownLatch(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var pending = new TransactionTemplate(transactionManager).execute(status -> {
                testSettPessimisticLocking.lockTestSett(sett.id());
                var future = executor.submit(() -> {
                    started.countDown();
                    return organiseringStateService.opprett(sett.id(), organiseringStateData(0, "synthetic-lock-worker"));
                });
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for domain mutation", e);
                }
                assertThatThrownBy(() -> future.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                return future;
            });

            assertThat(pending).isNotNull();
            assertThat(pending.get(5, TimeUnit.SECONDS).data().fid()).isEqualTo("synthetic-lock-worker");
        }

        assertThat(organiseringStateService.finnAlle(sett.id())).hasSize(1);
    }

    private TestSett createNamedSet(String name) {
        return testSettService.opprett(new TestSett.Data(name, "synthetic description"));
    }

    private Rows createRows(long testSettId, String suffix) {
        var orgenhetHigh = orgenhetStateService.opprett(testSettId, orgenhetData(8, suffix + "-org-high"));
        var orgenhetLow = orgenhetStateService.opprett(testSettId, orgenhetData(2, suffix + "-org-low"));
        var orgenhetLinked = orgenhetEvtsrcMsgService.opprett(testSettId, orgenhetMessage(orgenhetHigh.id(), 9, suffix));
        var orgenhetStandalone = orgenhetEvtsrcMsgService.opprett(testSettId, orgenhetMessage(null, 1, suffix));

        var ressursHigh = ressursStateService.opprett(testSettId, ressursData(8, suffix + "-ressurs-high"));
        var ressursLow = ressursStateService.opprett(testSettId, ressursData(2, suffix + "-ressurs-low"));
        var ressursLinked = ressursEvtsrcMsgService.opprett(testSettId, ressursMessage(ressursHigh.id(), 9, suffix));
        var ressursStandalone = ressursEvtsrcMsgService.opprett(testSettId, ressursMessage(null, 1, suffix));

        var organiseringHigh = organiseringStateService.opprett(testSettId, organiseringStateData(8, suffix + "-organisering-high"));
        var organiseringLow = organiseringStateService.opprett(testSettId, organiseringStateData(2, suffix + "-organisering-low"));
        var organiseringLinked = organiseringEvtsrcMsgService.opprett(
            testSettId,
            organiseringMessage(organiseringHigh.id(), 9, suffix)
        );
        var organiseringStandalone = organiseringEvtsrcMsgService.opprett(testSettId, organiseringMessage(null, 1, suffix));

        var lederHigh = orgEnhetLederStateService.opprett(testSettId, orgEnhetLederData(8, suffix + "-leder-high"));
        var lederLow = orgEnhetLederStateService.opprett(testSettId, orgEnhetLederData(2, suffix + "-leder-low"));
        var lederLinked = orgEnhetLederEvtsrcMsgService.opprett(testSettId, orgEnhetLederMessage(lederHigh.id(), 9, suffix));
        var lederStandalone = orgEnhetLederEvtsrcMsgService.opprett(testSettId, orgEnhetLederMessage(null, 1, suffix));

        var orgtilknytningHigh = orgtilknytningStateService.opprett(
            testSettId,
            orgtilknytningData(8, suffix + "-orgtilknytning-high")
        );
        var orgtilknytningLow = orgtilknytningStateService.opprett(
            testSettId,
            orgtilknytningData(2, suffix + "-orgtilknytning-low")
        );
        var orgtilknytningLinked = orgtilknytningEvtsrcMsgService.opprett(
            testSettId,
            orgtilknytningMessage(orgtilknytningHigh.id(), 9, suffix)
        );
        var orgtilknytningStandalone = orgtilknytningEvtsrcMsgService.opprett(
            testSettId,
            orgtilknytningMessage(null, 1, suffix)
        );

        return new Rows(
            List.of(orgenhetHigh, orgenhetLow), List.of(orgenhetLinked, orgenhetStandalone),
            List.of(ressursHigh, ressursLow), List.of(ressursLinked, ressursStandalone),
            List.of(organiseringHigh, organiseringLow), List.of(organiseringLinked, organiseringStandalone),
            List.of(lederHigh, lederLow), List.of(lederLinked, lederStandalone),
            List.of(orgtilknytningHigh, orgtilknytningLow), List.of(orgtilknytningLinked, orgtilknytningStandalone)
        );
    }

    private static OrgenhetState.Data orgenhetData(long sortIdx, String fid) {
        return new OrgenhetState.Data(
            sortIdx, TIMESTAMP, fid, "synthetic-org-unit-id", "Synthetic unit",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), OrgEnhetsType.values()[0], NomNivaa.values()[0],
            "synthetic-unique-agresso", "synthetic-agresso", "synthetic-level", "synthetic-type", "synthetic-access", "synthetic-cost"
        );
    }

    private static RessursState.Data ressursData(long sortIdx, String fid) {
        return new RessursState.Data(
            sortIdx, TIMESTAMP, fid, "synthetic-person-ident", "synthetic-navident",
            Sektor.values()[0], "Synthetic", null, "Person", "synthetic-phone", TelefonType.values()[0],
            "synthetic-primary-phone", null, LocalDate.of(2026, 1, 1), "synthetic-workplace",
            List.of("synthetic-workplace-a", "synthetic-workplace-b"), IdentType.values()[0], "synthetic-state-editor",
            Tilknytningtype.values()[0]
        );
    }

    private static OrganiseringState.Data organiseringStateData(long sortIdx, String fid) {
        return new OrganiseringState.Data(
            sortIdx, TIMESTAMP, fid, "synthetic-organisering-id", "Synthetic Org",
            "synthetic-parent-id", "Synthetic Parent", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
    }

    private static OrgEnhetLederState.Data orgEnhetLederData(long sortIdx, String fid) {
        return new OrgEnhetLederState.Data(
            sortIdx, TIMESTAMP, fid, "synthetic-navident", "synthetic-org-unit-id",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "synthetic-state-editor"
        );
    }

    private static OrgtilknytningState.Data orgtilknytningData(long sortIdx, String fid) {
        return new OrgtilknytningState.Data(
            sortIdx, TIMESTAMP, fid, "synthetic-navident", "synthetic-org-unit-id",
            Sektor.values()[0], true, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), Tilknytningtype.values()[0],
            "synthetic-primary-workplace", List.of("synthetic-workplace-a", "synthetic-workplace-b"), null,
            "synthetic-state-editor"
        );
    }

    private static OrgenhetEvtsrcMsg.Data orgenhetMessage(Long stateId, long sortIdx, String suffix) {
        return new OrgenhetEvtsrcMsg.Data(
            stateId, sortIdx, TIMESTAMP, "synthetic.Event", "synthetic-key-" + suffix,
            "synthetic-private-payload-" + suffix
        );
    }

    private static RessursEvtsrcMsg.Data ressursMessage(Long stateId, long sortIdx, String suffix) {
        return new RessursEvtsrcMsg.Data(
            stateId, sortIdx, TIMESTAMP, "synthetic.Event", "synthetic-key-" + suffix,
            "synthetic-private-payload-" + suffix
        );
    }

    private static OrganiseringEvtsrcMsg.Data organiseringMessage(Long stateId, long sortIdx, String suffix) {
        return new OrganiseringEvtsrcMsg.Data(
            stateId, sortIdx, TIMESTAMP, "synthetic.Event", "synthetic-key-" + suffix,
            "synthetic-private-payload-" + suffix
        );
    }

    private static OrgEnhetLederEvtsrcMsg.Data orgEnhetLederMessage(Long stateId, long sortIdx, String suffix) {
        return new OrgEnhetLederEvtsrcMsg.Data(
            stateId, sortIdx, TIMESTAMP, "synthetic.Event", "synthetic-key-" + suffix,
            "synthetic-private-payload-" + suffix
        );
    }

    private static OrgtilknytningEvtsrcMsg.Data orgtilknytningMessage(Long stateId, long sortIdx, String suffix) {
        return new OrgtilknytningEvtsrcMsg.Data(
            stateId, sortIdx, TIMESTAMP, "synthetic.Event", "synthetic-key-" + suffix,
            "synthetic-private-payload-" + suffix
        );
    }

    private void assertChildTableEmpty(String table, long testSettId) {
        var count = jdbcClient.sql("SELECT count(*) FROM " + table + " WHERE test_sett_id = :testSettId")
            .param("testSettId", testSettId).query(Long.class).single();
        assertThat(count).as("Rows remaining in %s", table).isZero();
    }

    private static void assertEveryCollectionBelongsTo(long testSettId, KomplettTestSett complete) {
        assertThat(complete.states()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.meldinger()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.ressursStates()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.ressursMeldinger()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.organiseringStates()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.organiseringMeldinger()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.orgenhetLederStates()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.orgenhetLederMeldinger()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.orgtilknytningStates()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
        assertThat(complete.orgtilknytningMeldinger()).allSatisfy(row -> assertThat(row.testSettId()).isEqualTo(testSettId));
    }

    private static <T> List<T> ordered(List<T> rows, java.util.function.Function<T, Object> data) {
        return rows.stream().sorted(Comparator.comparingLong(row -> sortIndex(data.apply(row)))).toList();
    }

    private static long sortIndex(Object data) {
        return switch (data) {
            case OrgenhetState.Data row -> row.sortIdx();
            case OrgenhetEvtsrcMsg.Data row -> row.sortIdx();
            case RessursState.Data row -> row.sortIdx();
            case RessursEvtsrcMsg.Data row -> row.sortIdx();
            case OrganiseringState.Data row -> row.sortIdx();
            case OrganiseringEvtsrcMsg.Data row -> row.sortIdx();
            case OrgEnhetLederState.Data row -> row.sortIdx();
            case OrgEnhetLederEvtsrcMsg.Data row -> row.sortIdx();
            case OrgtilknytningState.Data row -> row.sortIdx();
            case OrgtilknytningEvtsrcMsg.Data row -> row.sortIdx();
            default -> throw new IllegalArgumentException("Unsupported test data type");
        };
    }

    private record Rows(
        List<OrgenhetState> orgenhetStates,
        List<OrgenhetEvtsrcMsg> orgenhetMeldinger,
        List<RessursState> ressursStates,
        List<RessursEvtsrcMsg> ressursMeldinger,
        List<OrganiseringState> organiseringStates,
        List<OrganiseringEvtsrcMsg> organiseringMeldinger,
        List<OrgEnhetLederState> orgEnhetLederStates,
        List<OrgEnhetLederEvtsrcMsg> orgEnhetLederMeldinger,
        List<OrgtilknytningState> orgtilknytningStates,
        List<OrgtilknytningEvtsrcMsg> orgtilknytningMeldinger
    ) {}
}
