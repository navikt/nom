package no.nav.nom.apps.testdata.repository;

import no.nav.nom.apps.testdata.model.Audit;
import no.nav.nom.apps.testdata.model.TestSett;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class TestSettRepository {
    private static final String COLUMNS = "id, navn, beskrivelse, opprettet, opprettet_av, sist_endret, sist_endret_av";
    private static final RowMapper<TestSett> ROW_MAPPER = TestSettRepository::mapRow;

    private final JdbcClient jdbcClient;

    public TestSettRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public TestSett create(TestSett.Data data, Audit.Endring endring) {
        String sql = """
            INSERT INTO test_sett (navn, beskrivelse, opprettet, opprettet_av, sist_endret, sist_endret_av)
            VALUES (:navn, :beskrivelse, :opprettet, :opprettetAv, :sistEndret, :sistEndretAv)
            RETURNING %s
            """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("navn", data.navn())
            .param("beskrivelse", data.beskrivelse())
            .param("opprettet", endring.tidspunkt())
            .param("opprettetAv", endring.bruker())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .query(ROW_MAPPER)
            .single();
    }

    public Optional<TestSett> findById(long id) {
        return jdbcClient.sql("SELECT %s FROM test_sett WHERE id = :id".formatted(COLUMNS))
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public List<TestSett> findAll() {
        return jdbcClient.sql("SELECT %s FROM test_sett ORDER BY id".formatted(COLUMNS))
            .query(ROW_MAPPER)
            .list();
    }

    public Optional<TestSett> findByIdForUpdate(long id) {
        return jdbcClient.sql("SELECT %s FROM test_sett WHERE id = :id FOR UPDATE".formatted(COLUMNS))
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public Optional<TestSett> update(long id, TestSett.Data data, Audit.Endring endring) {
        String sql = """
            UPDATE test_sett SET navn = :navn, beskrivelse = :beskrivelse, sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
            WHERE id = :id
            RETURNING %s
            """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("navn", data.navn())
            .param("beskrivelse", data.beskrivelse())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public int delete(long id) {
        return jdbcClient.sql("DELETE FROM test_sett WHERE id = :id")
            .param("id", id)
            .update();
    }

    private static TestSett mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TestSett(
            rs.getLong("id"),
            new TestSett.Data(
                rs.getString("navn"),
                rs.getString("beskrivelse")
            ),
            Audit.mapAudit(rs)
        );
    }
}
