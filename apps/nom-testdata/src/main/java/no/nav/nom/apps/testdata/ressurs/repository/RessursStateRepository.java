package no.nav.nom.apps.testdata.ressurs.repository;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.apps.testdata.common.repository.JdbcArrays;
import no.nav.nom.apps.testdata.ressurs.RessursState;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.Tilknytningtype;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class RessursStateRepository {
    private static final String COLUMNS =
        """
        id, test_sett_id, sort_idx, state_timestamp,
        fid, personident, navident, sektor, fornavn, mellomnavn, etternavn,
        jobbtelefon_nummer, jobbtelefon_type, primary_telefonnummer, sluttdato, startdato,
        primary_fysisk_brukersted, fysiske_brukersteder, ident_type, state_sist_endret_av, tilknytningtype,
        opprettet, opprettet_av, sist_endret, sist_endret_av
        """;
    private static final RowMapper<RessursState> ROW_MAPPER = RessursStateRepository::mapRow;

    private final JdbcClient jdbcClient;

    public RessursStateRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public RessursState create(long testSettId, RessursState.Data data, Audit.Endring endring) {
        String sql = """
                     INSERT INTO ressurs_state (
                         test_sett_id, sort_idx, state_timestamp, fid, personident, navident, sektor,
                         fornavn, mellomnavn, etternavn, jobbtelefon_nummer, jobbtelefon_type, primary_telefonnummer,
                         sluttdato, startdato, primary_fysisk_brukersted, fysiske_brukersteder, ident_type,
                         state_sist_endret_av, tilknytningtype,
                         opprettet, opprettet_av, sist_endret, sist_endret_av
                     )
                     VALUES (
                         :testSettId, :sortIdx, :stateTimestamp, :fid, :personident, :navident, :sektor,
                         :fornavn, :mellomnavn, :etternavn, :jobbtelefonNummer, :jobbtelefonType, :primaryTelefonnummer,
                         :sluttdato, :startdato, :primaryFysiskBrukersted, :fysiskeBrukersteder, :identType,
                         :stateSistEndretAv, :tilknytningtype,
                         :opprettet, :opprettetAv, :sistEndret, :sistEndretAv
                     )
                     RETURNING %s
                     """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("testSettId", testSettId)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("personident", data.personident())
            .param("navident", data.navident())
            .param("sektor", enumName(data.sektor()))
            .param("fornavn", data.fornavn())
            .param("mellomnavn", data.mellomnavn())
            .param("etternavn", data.etternavn())
            .param("jobbtelefonNummer", data.jobbtelefonNummer())
            .param("jobbtelefonType", enumName(data.jobbtelefonType()))
            .param("primaryTelefonnummer", data.primaryTelefonnummer())
            .param("sluttdato", data.sluttdato())
            .param("startdato", data.startdato())
            .param("primaryFysiskBrukersted", data.primaryFysiskBrukersted())
            .param("fysiskeBrukersteder", JdbcArrays.textArray(data.fysiskeBrukersteder()), Types.ARRAY)
            .param("identType", enumName(data.identType()))
            .param("stateSistEndretAv", data.stateSistEndretAv())
            .param("tilknytningtype", enumName(data.tilknytningtype()))
            .param("opprettet", endring.tidspunkt())
            .param("opprettetAv", endring.bruker())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .query(ROW_MAPPER)
            .single();
    }

    public Optional<RessursState> findById(long testSettId, long id) {
        return jdbcClient.sql("SELECT %s FROM ressurs_state WHERE test_sett_id = :testSettId AND id = :id".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public List<RessursState> findAll(long testSettId) {
        return jdbcClient.sql("SELECT %s FROM ressurs_state WHERE test_sett_id = :testSettId ORDER BY sort_idx".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .query(ROW_MAPPER)
            .list();
    }

    public List<RessursState> findByTestSetIdAndFid(long testSettId, String fid) {
        return jdbcClient.sql("""
                              SELECT %s
                              FROM ressurs_state
                              WHERE test_sett_id = :testSettId
                              AND fid = :fid
                              ORDER BY sort_idx
                              """.formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("fid", fid)
            .query(ROW_MAPPER)
            .list();
    }

    public Optional<RessursState> update(long testSettId, long id, RessursState.Data data, Audit.Endring endring) {
        String sql = """
                     UPDATE ressurs_state
                     SET sort_idx = :sortIdx, state_timestamp = :stateTimestamp,
                         fid = :fid, personident = :personident, navident = :navident, sektor = :sektor,
                         fornavn = :fornavn, mellomnavn = :mellomnavn, etternavn = :etternavn,
                         jobbtelefon_nummer = :jobbtelefonNummer, jobbtelefon_type = :jobbtelefonType,
                         primary_telefonnummer = :primaryTelefonnummer, sluttdato = :sluttdato, startdato = :startdato,
                         primary_fysisk_brukersted = :primaryFysiskBrukersted, fysiske_brukersteder = :fysiskeBrukersteder,
                         ident_type = :identType, state_sist_endret_av = :stateSistEndretAv, tilknytningtype = :tilknytningtype,
                         sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                     WHERE test_sett_id = :testSettId
                     AND id = :id
                     RETURNING %s
                     """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("personident", data.personident())
            .param("navident", data.navident())
            .param("sektor", enumName(data.sektor()))
            .param("fornavn", data.fornavn())
            .param("mellomnavn", data.mellomnavn())
            .param("etternavn", data.etternavn())
            .param("jobbtelefonNummer", data.jobbtelefonNummer())
            .param("jobbtelefonType", enumName(data.jobbtelefonType()))
            .param("primaryTelefonnummer", data.primaryTelefonnummer())
            .param("sluttdato", data.sluttdato())
            .param("startdato", data.startdato())
            .param("primaryFysiskBrukersted", data.primaryFysiskBrukersted())
            .param("fysiskeBrukersteder", JdbcArrays.textArray(data.fysiskeBrukersteder()), Types.ARRAY)
            .param("identType", enumName(data.identType()))
            .param("stateSistEndretAv", data.stateSistEndretAv())
            .param("tilknytningtype", enumName(data.tilknytningtype()))
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public int delete(long testSettId, long id) {
        return jdbcClient.sql("DELETE FROM ressurs_state WHERE test_sett_id = :testSettId AND id = :id")
            .param("testSettId", testSettId)
            .param("id", id)
            .update();
    }

    public int deleteAllByTestSetId(long testSettId) {
        return jdbcClient.sql("DELETE FROM ressurs_state WHERE test_sett_id = :testSettId")
            .param("testSettId", testSettId)
            .update();
    }

    public int updateSortIndex(long testSettId, long id, long sortIdx, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE ressurs_state
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
        jdbcClient.sql("SET CONSTRAINTS uq_ressurs_state_sort_idx DEFERRED").update();
    }

    private static RessursState mapRow(ResultSet rs, int rowNum) throws SQLException {
        String sektor = rs.getString("sektor");
        String jobbtelefonType = rs.getString("jobbtelefon_type");
        String identType = rs.getString("ident_type");
        String tilknytningtype = rs.getString("tilknytningtype");
        return new RessursState(
            rs.getLong("id"),
            rs.getLong("test_sett_id"),
            new RessursState.Data(
                rs.getLong("sort_idx"),
                rs.getObject("state_timestamp", java.time.LocalDateTime.class),
                rs.getString("fid"),
                rs.getString("personident"),
                rs.getString("navident"),
                sektor == null ? null : Sektor.valueOf(sektor),
                rs.getString("fornavn"),
                rs.getString("mellomnavn"),
                rs.getString("etternavn"),
                rs.getString("jobbtelefon_nummer"),
                jobbtelefonType == null ? null : TelefonType.valueOf(jobbtelefonType),
                rs.getString("primary_telefonnummer"),
                rs.getObject("sluttdato", java.time.LocalDate.class),
                rs.getObject("startdato", java.time.LocalDate.class),
                rs.getString("primary_fysisk_brukersted"),
                JdbcArrays.readTextArray(rs, "fysiske_brukersteder"),
                identType == null ? null : IdentType.valueOf(identType),
                rs.getString("state_sist_endret_av"),
                tilknytningtype == null ? null : Tilknytningtype.valueOf(tilknytningtype)
            ),
            Audit.mapAudit(rs)
        );
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
