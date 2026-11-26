package no.nav.nom.apps.testdata.organisering.repository;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.apps.testdata.organisering.OrganiseringEvtsrcMsg;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class OrganiseringEvtsrcMsgRepository {
    private static final String COLUMNS =
        """
        id, test_sett_id, organisering_state_id, sort_idx,
        evtsrc_timestamp, object_type, evtsrc_key, evtsrc_message,
        opprettet, opprettet_av, sist_endret, sist_endret_av
        """;
    private static final RowMapper<OrganiseringEvtsrcMsg> ROW_MAPPER = OrganiseringEvtsrcMsgRepository::mapRow;

    private final JdbcClient jdbcClient;

    public OrganiseringEvtsrcMsgRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public OrganiseringEvtsrcMsg create(long testSettId, OrganiseringEvtsrcMsg.Data data, Audit.Endring endring) {
        String sql =
             """
             INSERT INTO organisering_evtsrc_msg (
                 test_sett_id, organisering_state_id, sort_idx, evtsrc_timestamp, object_type, evtsrc_key, evtsrc_message,
                 opprettet, opprettet_av, sist_endret, sist_endret_av
             )
             VALUES (
                 :testSettId, :organiseringStateId, :sortIdx, :evtsrcTimestamp, :objectType, :evtsrcKey, :evtsrcMessage,
                 :opprettet, :opprettetAv, :sistEndret, :sistEndretAv
             )
             RETURNING %s
             """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("testSettId", testSettId)
            .param("organiseringStateId", data.organiseringStateId())
            .param("sortIdx", data.sortIdx())
            .param("evtsrcTimestamp", data.evtsrcTimestamp())
            .param("objectType", data.objectType())
            .param("evtsrcKey", data.evtsrcKey())
            .param("evtsrcMessage", data.evtsrcMessage())
            .param("opprettet", endring.tidspunkt())
            .param("opprettetAv", endring.bruker())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .query(ROW_MAPPER)
            .single();
    }

    public Optional<OrganiseringEvtsrcMsg> findById(long testSettId, long id) {
        return jdbcClient.sql("SELECT %s FROM organisering_evtsrc_msg WHERE test_sett_id = :testSettId AND id = :id".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public List<OrganiseringEvtsrcMsg> findAll(long testSettId) {
        return jdbcClient.sql("SELECT %s FROM organisering_evtsrc_msg WHERE test_sett_id = :testSettId ORDER BY sort_idx".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .query(ROW_MAPPER)
            .list();
    }

    public Optional<OrganiseringEvtsrcMsg> update(
        long testSettId,
        long id,
        OrganiseringEvtsrcMsg.Data data,
        Audit.Endring endring
    ) {
        String sql = """
                     UPDATE organisering_evtsrc_msg
                     SET organisering_state_id = :organiseringStateId, sort_idx = :sortIdx,
                         evtsrc_timestamp = :evtsrcTimestamp, object_type = :objectType, evtsrc_key = :evtsrcKey,
                         evtsrc_message = :evtsrcMessage, sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                     WHERE test_sett_id = :testSettId
                     AND id = :id
                     RETURNING %s
                     """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("organiseringStateId", data.organiseringStateId())
            .param("sortIdx", data.sortIdx())
            .param("evtsrcTimestamp", data.evtsrcTimestamp())
            .param("objectType", data.objectType())
            .param("evtsrcKey", data.evtsrcKey())
            .param("evtsrcMessage", data.evtsrcMessage())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public int delete(long testSettId, long id) {
        return jdbcClient.sql("DELETE FROM organisering_evtsrc_msg WHERE test_sett_id = :testSettId AND id = :id")
            .param("testSettId", testSettId)
            .param("id", id)
            .update();
    }

    public int deleteAllByTestSetId(long testSettId) {
        return jdbcClient.sql("DELETE FROM organisering_evtsrc_msg WHERE test_sett_id = :testSettId")
            .param("testSettId", testSettId)
            .update();
    }

    public int updateSortIndex(long testSettId, long id, long sortIdx, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE organisering_evtsrc_msg
                              SET sort_idx = :sortIdx, sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                              WHERE test_sett_id = :testSettId
                              AND id = :id
                              """)
            .param("sortIdx", sortIdx)
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("id", id)
            .update();
    }

    public void deferSortOrderConstraint() {
        jdbcClient.sql("SET CONSTRAINTS uq_organisering_evtsrc_msg_sort_idx DEFERRED").update();
    }

    public int deleteByTestSetIdAndStateId(long testSettId, long stateId) {
        return jdbcClient.sql("DELETE FROM organisering_evtsrc_msg WHERE test_sett_id = :testSettId AND organisering_state_id = :stateId")
            .param("testSettId", testSettId)
            .param("stateId", stateId)
            .update();
    }

    public int detachFromState(long testSettId, long stateId, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE organisering_evtsrc_msg
                              SET organisering_state_id = NULL, sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                              WHERE test_sett_id = :testSettId
                              AND organisering_state_id = :stateId
                              """)
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("stateId", stateId)
            .update();
    }

    public int deleteAllWithStateReferenceByTestSetId(long testSettId) {
        return jdbcClient.sql("DELETE FROM organisering_evtsrc_msg WHERE test_sett_id = :testSettId AND organisering_state_id IS NOT NULL")
            .param("testSettId", testSettId)
            .update();
    }

    public int detachAllStateReferences(long testSettId, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE organisering_evtsrc_msg
                              SET organisering_state_id = NULL, sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                              WHERE test_sett_id = :testSettId
                              AND organisering_state_id IS NOT NULL
                              """)
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .update();
    }

    private static OrganiseringEvtsrcMsg mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OrganiseringEvtsrcMsg(
            rs.getLong("id"),
            rs.getLong("test_sett_id"),
            new OrganiseringEvtsrcMsg.Data(
                rs.getObject("organisering_state_id", Long.class),
                rs.getLong("sort_idx"),
                rs.getObject("evtsrc_timestamp", java.time.LocalDateTime.class),
                rs.getString("object_type"),
                rs.getString("evtsrc_key"),
                rs.getString("evtsrc_message")
            ),
            Audit.mapAudit(rs)
        );
    }
}
