package no.nav.nom.apps.testdata;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetEvtsrcMsg;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetState;
import no.nav.nom.apps.testdata.testsett.TestSett;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryTest extends RepositoryDomainTestBase {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 1, 2, 12, 0);
    private static final Audit.Endring CREATED = new Audit.Endring(CREATED_AT, "synthetic-actor");
    private static final Audit.Endring UPDATED = new Audit.Endring(UPDATED_AT, "synthetic-editor");

    @Test
    void postgres18MigrationCreatesAllThreeTables() {
        assertThat(POSTGRES.getDockerImageName()).isEqualTo("postgres:18");
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success")
            .query(Integer.class).single())
            .isEqualTo(1);
        assertThat(jdbcClient.sql(
            """
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name IN ('test_sett', 'orgenhet_state', 'orgenhet_evtsrc_msg')
                """
        ).query(Integer.class).single()).isEqualTo(3);
    }

    @Test
    void testSettRepositorySupportsCreateReadListUpdateAndDelete() {
        var originalData = new TestSett.Data("synthetic set", "first description");
        var created = testSettRepository.create(originalData, CREATED);

        assertThat(testSettRepository.findById(created.id())).contains(created);
        assertThat(testSettRepository.findAll()).containsExactly(created);

        var changedData = new TestSett.Data("renamed set", "updated description");
        var updated = testSettRepository.update(created.id(), changedData, UPDATED).orElseThrow();
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.data()).isEqualTo(changedData);
        assertThat(updated.audit()).isEqualTo(new Audit(CREATED_AT, "synthetic-actor", UPDATED_AT, "synthetic-editor"));

        assertThat(testSettRepository.delete(created.id())).isEqualTo(1);
        assertThat(testSettRepository.findById(created.id())).isEmpty();
    }

    @Test
    void stateRepositoryMapsTypedFieldsAndPreservesCreationAuditOnUpdate() {
        var testSett = createTestSett("state test set");
        var originalData = stateData(0, LocalDateTime.of(2026, 1, 3, 10, 0), "org-001");
        var created = orgenhetStateRepository.create(testSett.id(), originalData, CREATED);

        assertThat(created.data()).isEqualTo(originalData);
        assertThat(orgenhetStateRepository.findById(testSett.id(), created.id())).contains(created);
        assertThat(orgenhetStateRepository.findAll(testSett.id())).containsExactly(created);

        var changedData = new OrgenhetState.Data(
            0, LocalDateTime.of(2026, 1, 4, 10, 0), "fid-updated",
            "org-002", "Synthetic unit updated", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31),
            OrgEnhetsType.NAV_KONTOR, NomNivaa.DRIFTSENHET, "unique-updated", "agresso-updated",
            "org-level-updated", "type-updated", "access-updated", "cost-updated"
        );
        var updated = orgenhetStateRepository.update(testSett.id(), created.id(), changedData, UPDATED).orElseThrow();
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.data()).isEqualTo(changedData);
        assertThat(updated.audit()).isEqualTo(new Audit(CREATED_AT, "synthetic-actor", UPDATED_AT, "synthetic-editor"));

        assertThat(orgenhetStateRepository.delete(testSett.id(), created.id())).isEqualTo(1);
        assertThat(orgenhetStateRepository.findById(testSett.id(), created.id())).isEmpty();
    }

    @Test
    void messageRepositoryPreservesRawTextAndAllowsNullStateReference() {
        var testSett = createTestSett("message test set");
        String rawMessage = "\n  {\n not-json \n}  \n";
        var originalData = new OrgenhetEvtsrcMsg.Data(
            null, 0, LocalDateTime.of(2026, 1, 5, 9, 0),
            "synthetic-object", "synthetic-key", rawMessage
        );
        var created = orgenhetEvtsrcMsgRepository.create(testSett.id(), originalData, CREATED);

        assertThat(created.data()).isEqualTo(originalData);
        assertThat(created.data().evtsrcMessage()).isEqualTo(rawMessage);
        assertThat(orgenhetEvtsrcMsgRepository.findById(testSett.id(), created.id())).contains(created);
        assertThat(orgenhetEvtsrcMsgRepository.findAll(testSett.id())).containsExactly(created);

        var changedData = new OrgenhetEvtsrcMsg.Data(
            null, 0, LocalDateTime.of(2026, 1, 6, 9, 0),
            "updated-object", "updated-key", "  plain text\n with whitespace  "
        );
        var updated = orgenhetEvtsrcMsgRepository.update(testSett.id(), created.id(), changedData, UPDATED).orElseThrow();
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.data()).isEqualTo(changedData);
        assertThat(updated.audit()).isEqualTo(new Audit(CREATED_AT, "synthetic-actor", UPDATED_AT, "synthetic-editor"));

        assertThat(orgenhetEvtsrcMsgRepository.delete(testSett.id(), created.id())).isEqualTo(1);
        assertThat(orgenhetEvtsrcMsgRepository.findById(testSett.id(), created.id())).isEmpty();
    }

    @Test
    void stateAndMessageListsAreSortedBySortIndexRatherThanTimestamp() {
        var testSett = createTestSett("sorting test set");
        var stateWithIndexOne = orgenhetStateRepository.create(
            testSett.id(),
            stateData(1, LocalDateTime.of(2026, 1, 1, 8, 0), "org-same"), CREATED
        );
        var stateWithIndexZero = orgenhetStateRepository.create(
            testSett.id(),
            stateData(0, LocalDateTime.of(2026, 1, 2, 8, 0), "org-same"), CREATED
        );
        var msgWithIndexOne = orgenhetEvtsrcMsgRepository.create(
            testSett.id(),
            msgData(1, LocalDateTime.of(2026, 1, 1, 8, 0), "message one"), CREATED
        );
        var msgWithIndexZero = orgenhetEvtsrcMsgRepository.create(
            testSett.id(),
            msgData(0, LocalDateTime.of(2026, 1, 2, 8, 0), "message zero"), CREATED
        );

        assertThat(orgenhetStateRepository.findAll(testSett.id()))
            .containsExactly(stateWithIndexZero, stateWithIndexOne);
        assertThat(orgenhetEvtsrcMsgRepository.findAll(testSett.id()))
            .containsExactly(msgWithIndexZero, msgWithIndexOne);
    }

    @Test
    void repeatedOrgIdsAndTimestampsAreAllowed() {
        var testSett = createTestSett("repeated values test set");
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 7, 8, 0);
        orgenhetStateRepository.create(testSett.id(), stateData(0, timestamp, "org-repeated"), CREATED);
        orgenhetStateRepository.create(testSett.id(), stateData(1, timestamp, "org-repeated"), CREATED);
        orgenhetEvtsrcMsgRepository.create(testSett.id(), msgData(0, timestamp, "first"), CREATED);
        orgenhetEvtsrcMsgRepository.create(testSett.id(), msgData(1, timestamp, "second"), CREATED);

        assertThat(orgenhetStateRepository.findByTestSetIdAndOrgUnitId(testSett.id(), "org-repeated")).hasSize(2);
        assertThat(orgenhetEvtsrcMsgRepository.findAll(testSett.id())).hasSize(2);
    }

    @Test
    void duplicateSortIndexesAreRejectedOnlyWithinTheSameTableAndTestSet() {
        var firstSet = createTestSett("first set");
        var secondSet = createTestSett("second set");
        orgenhetStateRepository.create(firstSet.id(), stateData(0, CREATED_AT, "org-state"), CREATED);

        assertThatThrownBy(() -> orgenhetStateRepository.create(
            firstSet.id(),
            stateData(0, CREATED_AT.plusHours(1), "org-state-duplicate"), CREATED
        ))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(orgenhetStateRepository.create(secondSet.id(), stateData(0, CREATED_AT, "org-state-other-set"), CREATED))
            .isNotNull();
        assertThat(orgenhetEvtsrcMsgRepository.create(firstSet.id(), msgData(0, CREATED_AT, "same index, other table"), CREATED))
            .isNotNull();
        assertThat(orgenhetEvtsrcMsgRepository.create(secondSet.id(), msgData(0, CREATED_AT, "same index, other set"), CREATED))
            .isNotNull();
        assertThatThrownBy(() -> orgenhetEvtsrcMsgRepository.create(
            firstSet.id(),
            msgData(0, CREATED_AT.plusHours(1), "duplicate message index"), CREATED
        ))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private TestSett createTestSett(String name) {
        return testSettRepository.create(new TestSett.Data(name, "synthetic description"), CREATED);
    }

    private static OrgenhetState.Data stateData(long sortIdx, LocalDateTime timestamp, String orgenhetId) {
        return new OrgenhetState.Data(
            sortIdx, timestamp, "synthetic-fid", orgenhetId, "Synthetic unit",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), OrgEnhetsType.DIREKTORAT,
            NomNivaa.LINJEENHET, "synthetic-unique-agresso", "synthetic-agresso", "synthetic-level",
            "synthetic-type", "synthetic-access", "synthetic-cost"
        );
    }

    private static OrgenhetEvtsrcMsg.Data msgData(long sortIdx, LocalDateTime timestamp, String message) {
        return new OrgenhetEvtsrcMsg.Data(null, sortIdx, timestamp, "synthetic-object", "synthetic-key", message);
    }
}
