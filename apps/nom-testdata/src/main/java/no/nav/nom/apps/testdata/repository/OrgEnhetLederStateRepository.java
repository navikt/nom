package no.nav.nom.apps.testdata.repository;

import no.nav.nom.apps.testdata.model.Audit;
import no.nav.nom.apps.testdata.model.OrgEnhetLederState;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class OrgEnhetLederStateRepository {
    private static final String COLUMNS =
        """
        id, test_sett_id, sort_idx, state_timestamp, fid, navident, org_enhet_id,
        startdato, sluttdato, state_sist_endret_av,
        opprettet, opprettet_av, sist_endret, sist_endret_av
        """;
    private static final RowMapper<OrgEnhetLederState> ROW_MAPPER = OrgEnhetLederStateRepository::mapRow;

    private final JdbcClient jdbcClient;

    public OrgEnhetLederStateRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public OrgEnhetLederState create(long testSettId, OrgEnhetLederState.Data data, Audit.Endring endring) {
        String sql = """
                     INSERT INTO orgenhet_leder_state (
                         test_sett_id, sort_idx, state_timestamp, fid, navident, org_enhet_id,
                         startdato, sluttdato, state_sist_endret_av,
                         opprettet, opprettet_av, sist_endret, sist_endret_av
                     )
                     VALUES (
                         :testSettId, :sortIdx, :stateTimestamp, :fid, :navident, :orgEnhetId,
                         :startdato, :sluttdato, :stateSistEndretAv,
                         :opprettet, :opprettetAv, :sistEndret, :sistEndretAv
                     )
                     RETURNING %s
                     """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("testSettId", testSettId)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("navident", data.navident())
            .param("orgEnhetId", data.orgEnhetId())
            .param("startdato", data.startdato())
            .param("sluttdato", data.sluttdato())
            .param("stateSistEndretAv", data.stateSistEndretAv())
            .param("opprettet", endring.tidspunkt())
            .param("opprettetAv", endring.bruker())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .query(ROW_MAPPER)
            .single();
    }

    public Optional<OrgEnhetLederState> findById(long testSettId, long id) {
        return jdbcClient.sql("SELECT %s FROM orgenhet_leder_state WHERE test_sett_id = :testSettId AND id = :id".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public List<OrgEnhetLederState> findAll(long testSettId) {
        return jdbcClient.sql("SELECT %s FROM orgenhet_leder_state WHERE test_sett_id = :testSettId ORDER BY sort_idx".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .query(ROW_MAPPER)
            .list();
    }

    public List<OrgEnhetLederState> findByTestSetIdAndFid(long testSettId, String fid) {
        return jdbcClient.sql("""
                              SELECT %s
                              FROM orgenhet_leder_state
                              WHERE test_sett_id = :testSettId
                              AND fid = :fid
                              ORDER BY sort_idx
                              """.formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("fid", fid)
            .query(ROW_MAPPER)
            .list();
    }

    public Optional<OrgEnhetLederState> update(long testSettId, long id, OrgEnhetLederState.Data data, Audit.Endring endring) {
        String sql = """
                     UPDATE orgenhet_leder_state
                     SET sort_idx = :sortIdx, state_timestamp = :stateTimestamp,
                         fid = :fid, navident = :navident, org_enhet_id = :orgEnhetId,
                         startdato = :startdato, sluttdato = :sluttdato, state_sist_endret_av = :stateSistEndretAv,
                         sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                     WHERE test_sett_id = :testSettId
                     AND id = :id
                     RETURNING %s
                     """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("navident", data.navident())
            .param("orgEnhetId", data.orgEnhetId())
            .param("startdato", data.startdato())
            .param("sluttdato", data.sluttdato())
            .param("stateSistEndretAv", data.stateSistEndretAv())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public int delete(long testSettId, long id) {
        return jdbcClient.sql("DELETE FROM orgenhet_leder_state WHERE test_sett_id = :testSettId AND id = :id")
            .param("testSettId", testSettId)
            .param("id", id)
            .update();
    }

    public int deleteAllByTestSetId(long testSettId) {
        return jdbcClient.sql("DELETE FROM orgenhet_leder_state WHERE test_sett_id = :testSettId")
            .param("testSettId", testSettId)
            .update();
    }

    public int updateSortIndex(long testSettId, long id, long sortIdx, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE orgenhet_leder_state
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
        jdbcClient.sql("SET CONSTRAINTS uq_orgenhet_leder_state_sort_idx DEFERRED").update();
    }

    private static OrgEnhetLederState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OrgEnhetLederState(
            rs.getLong("id"),
            rs.getLong("test_sett_id"),
            new OrgEnhetLederState.Data(
                rs.getLong("sort_idx"),
                rs.getObject("state_timestamp", java.time.LocalDateTime.class),
                rs.getString("fid"),
                rs.getString("navident"),
                rs.getString("org_enhet_id"),
                rs.getObject("startdato", java.time.LocalDate.class),
                rs.getObject("sluttdato", java.time.LocalDate.class),
                rs.getString("state_sist_endret_av")
            ),
            Audit.mapAudit(rs)
        );
    }
}
