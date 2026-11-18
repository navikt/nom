package no.nav.nom.apps.testdata.repository;

import no.nav.nom.apps.testdata.model.Audit;
import no.nav.nom.apps.testdata.model.OrganiseringState;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class OrganiseringStateRepository {
    private static final String COLUMNS =
        """
        id, test_sett_id, sort_idx, state_timestamp,
        fid, organisering_id, navn, id_over, navn_over, gyldig_fom, gyldig_tom,
        opprettet, opprettet_av, sist_endret, sist_endret_av
        """;
    private static final RowMapper<OrganiseringState> ROW_MAPPER = OrganiseringStateRepository::mapRow;

    private final JdbcClient jdbcClient;

    public OrganiseringStateRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public OrganiseringState create(long testSettId, OrganiseringState.Data data, Audit.Endring endring) {
        String sql =
            """
            INSERT INTO organisering_state (
                test_sett_id, sort_idx, state_timestamp, fid, organisering_id, navn, id_over, navn_over,
                gyldig_fom, gyldig_tom, opprettet, opprettet_av, sist_endret, sist_endret_av
            )
            VALUES (
                :testSettId, :sortIdx, :stateTimestamp, :fid, :organiseringId, :navn, :idOver, :navnOver,
                :gyldigFom, :gyldigTom, :opprettet, :opprettetAv, :sistEndret, :sistEndretAv
            )
            RETURNING %s
            """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("testSettId", testSettId)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("organiseringId", data.organiseringId())
            .param("navn", data.navn())
            .param("idOver", data.idOver())
            .param("navnOver", data.navnOver())
            .param("gyldigFom", data.gyldigFom())
            .param("gyldigTom", data.gyldigTom())
            .param("opprettet", endring.tidspunkt())
            .param("opprettetAv", endring.bruker())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .query(ROW_MAPPER)
            .single();
    }

    public Optional<OrganiseringState> findById(long testSettId, long id) {
        return jdbcClient.sql("SELECT %s FROM organisering_state WHERE test_sett_id = :testSettId AND id = :id".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public List<OrganiseringState> findAll(long testSettId) {
        return jdbcClient.sql("SELECT %s FROM organisering_state WHERE test_sett_id = :testSettId ORDER BY sort_idx".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .query(ROW_MAPPER)
            .list();
    }

    public List<OrganiseringState> findByTestSetIdAndFid(long testSettId, String fid) {
        return jdbcClient.sql("""
                              SELECT %s
                              FROM organisering_state
                              WHERE test_sett_id = :testSettId
                              AND fid = :fid
                              ORDER BY sort_idx
                              """.formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("fid", fid)
            .query(ROW_MAPPER)
            .list();
    }

    public Optional<OrganiseringState> update(long testSettId, long id, OrganiseringState.Data data, Audit.Endring endring) {
        String sql = """
                     UPDATE organisering_state
                     SET sort_idx = :sortIdx, state_timestamp = :stateTimestamp,
                         fid = :fid, organisering_id = :organiseringId, navn = :navn, id_over = :idOver, navn_over = :navnOver,
                         gyldig_fom = :gyldigFom, gyldig_tom = :gyldigTom,
                         sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                     WHERE test_sett_id = :testSettId
                     AND id = :id
                     RETURNING %s
                     """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("organiseringId", data.organiseringId())
            .param("navn", data.navn())
            .param("idOver", data.idOver())
            .param("navnOver", data.navnOver())
            .param("gyldigFom", data.gyldigFom())
            .param("gyldigTom", data.gyldigTom())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public int delete(long testSettId, long id) {
        return jdbcClient.sql("DELETE FROM organisering_state WHERE test_sett_id = :testSettId AND id = :id")
            .param("testSettId", testSettId)
            .param("id", id)
            .update();
    }

    public int deleteAllByTestSetId(long testSettId) {
        return jdbcClient.sql("DELETE FROM organisering_state WHERE test_sett_id = :testSettId")
            .param("testSettId", testSettId)
            .update();
    }

    public int updateSortIndex(long testSettId, long id, long sortIdx, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE organisering_state
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
        jdbcClient.sql("SET CONSTRAINTS uq_organisering_state_sort_idx DEFERRED").update();
    }

    private static OrganiseringState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OrganiseringState(
            rs.getLong("id"),
            rs.getLong("test_sett_id"),
            new OrganiseringState.Data(
                rs.getLong("sort_idx"),
                rs.getObject("state_timestamp", java.time.LocalDateTime.class),
                rs.getString("fid"),
                rs.getString("organisering_id"),
                rs.getString("navn"),
                rs.getString("id_over"),
                rs.getString("navn_over"),
                rs.getObject("gyldig_fom", java.time.LocalDate.class),
                rs.getObject("gyldig_tom", java.time.LocalDate.class)
            ),
            Audit.mapAudit(rs)
        );
    }
}
