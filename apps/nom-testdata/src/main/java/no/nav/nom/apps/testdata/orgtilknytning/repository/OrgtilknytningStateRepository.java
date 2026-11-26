package no.nav.nom.apps.testdata.orgtilknytning.repository;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.apps.testdata.common.repository.JdbcArrays;
import no.nav.nom.apps.testdata.orgtilknytning.OrgtilknytningState;
import no.nav.nom.contract.nom.Sektor;
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
public class OrgtilknytningStateRepository {
    private static final String COLUMNS =
        """
        id, test_sett_id, sort_idx, state_timestamp,
        fid, navident, org_enhet_id, sektor, er_daglig_oppfolging, gyldig_fom, gyldig_tom,
        orgtilknytning_type, primary_fysisk_brukersted_nummer, additional_fysiske_brukersteder_nummer,
        masseflyttet_fra_fid, state_sist_endret_av,
        opprettet, opprettet_av, sist_endret, sist_endret_av
        """;
    private static final RowMapper<OrgtilknytningState> ROW_MAPPER = OrgtilknytningStateRepository::mapRow;

    private final JdbcClient jdbcClient;

    public OrgtilknytningStateRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public OrgtilknytningState create(long testSettId, OrgtilknytningState.Data data, Audit.Endring endring) {
        String sql =
            """
            INSERT INTO orgtilknytning_state (
                test_sett_id, sort_idx, state_timestamp, fid, navident, org_enhet_id, sektor,
                er_daglig_oppfolging, gyldig_fom, gyldig_tom, orgtilknytning_type,
                primary_fysisk_brukersted_nummer, additional_fysiske_brukersteder_nummer,
                masseflyttet_fra_fid, state_sist_endret_av,
                opprettet, opprettet_av, sist_endret, sist_endret_av
            )
            VALUES (
                :testSettId, :sortIdx, :stateTimestamp, :fid, :navident, :orgEnhetId, :sektor,
                :erDagligOppfolging, :gyldigFom, :gyldigTom, :orgtilknytningType,
                :primaryFysiskBrukerstedNummer, :additionalFysiskeBrukerstederNummer,
                :masseflyttetFraFid, :stateSistEndretAv,
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
            .param("sektor", enumName(data.sektor()))
            .param("erDagligOppfolging", data.erDagligOppfolging())
            .param("gyldigFom", data.gyldigFom())
            .param("gyldigTom", data.gyldigTom())
            .param("orgtilknytningType", enumName(data.orgtilknytningType()))
            .param("primaryFysiskBrukerstedNummer", data.primaryFysiskBrukerstedNummer())
            .param("additionalFysiskeBrukerstederNummer", JdbcArrays.textArray(data.additionalFysiskeBrukerstederNummer()), Types.ARRAY)
            .param("masseflyttetFraFid", data.masseflyttetFraFid())
            .param("stateSistEndretAv", data.stateSistEndretAv())
            .param("opprettet", endring.tidspunkt())
            .param("opprettetAv", endring.bruker())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .query(ROW_MAPPER)
            .single();
    }

    public Optional<OrgtilknytningState> findById(long testSettId, long id) {
        return jdbcClient.sql("SELECT %s FROM orgtilknytning_state WHERE test_sett_id = :testSettId AND id = :id".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public List<OrgtilknytningState> findAll(long testSettId) {
        return jdbcClient.sql("SELECT %s FROM orgtilknytning_state WHERE test_sett_id = :testSettId ORDER BY sort_idx".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .query(ROW_MAPPER)
            .list();
    }

    public List<OrgtilknytningState> findByTestSetIdAndFid(long testSettId, String fid) {
        return jdbcClient.sql("""
                              SELECT %s
                              FROM orgtilknytning_state
                              WHERE test_sett_id = :testSettId
                              AND fid = :fid
                              ORDER BY sort_idx
                              """.formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("fid", fid)
            .query(ROW_MAPPER)
            .list();
    }

    public Optional<OrgtilknytningState> update(long testSettId, long id, OrgtilknytningState.Data data, Audit.Endring endring) {
        String sql =
            """
            UPDATE orgtilknytning_state
            SET sort_idx = :sortIdx, state_timestamp = :stateTimestamp,
                fid = :fid, navident = :navident, org_enhet_id = :orgEnhetId, sektor = :sektor,
                er_daglig_oppfolging = :erDagligOppfolging, gyldig_fom = :gyldigFom, gyldig_tom = :gyldigTom,
                orgtilknytning_type = :orgtilknytningType, primary_fysisk_brukersted_nummer = :primaryFysiskBrukerstedNummer,
                additional_fysiske_brukersteder_nummer = :additionalFysiskeBrukerstederNummer,
                masseflyttet_fra_fid = :masseflyttetFraFid, state_sist_endret_av = :stateSistEndretAv,
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
            .param("sektor", enumName(data.sektor()))
            .param("erDagligOppfolging", data.erDagligOppfolging())
            .param("gyldigFom", data.gyldigFom())
            .param("gyldigTom", data.gyldigTom())
            .param("orgtilknytningType", enumName(data.orgtilknytningType()))
            .param("primaryFysiskBrukerstedNummer", data.primaryFysiskBrukerstedNummer())
            .param("additionalFysiskeBrukerstederNummer", JdbcArrays.textArray(data.additionalFysiskeBrukerstederNummer()), Types.ARRAY)
            .param("masseflyttetFraFid", data.masseflyttetFraFid())
            .param("stateSistEndretAv", data.stateSistEndretAv())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public int delete(long testSettId, long id) {
        return jdbcClient.sql("DELETE FROM orgtilknytning_state WHERE test_sett_id = :testSettId AND id = :id")
            .param("testSettId", testSettId)
            .param("id", id)
            .update();
    }

    public int deleteAllByTestSetId(long testSettId) {
        return jdbcClient.sql("DELETE FROM orgtilknytning_state WHERE test_sett_id = :testSettId")
            .param("testSettId", testSettId)
            .update();
    }

    public int updateSortIndex(long testSettId, long id, long sortIdx, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE orgtilknytning_state
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
        jdbcClient.sql("SET CONSTRAINTS uq_orgtilknytning_state_sort_idx DEFERRED").update();
    }

    private static OrgtilknytningState mapRow(ResultSet rs, int rowNum) throws SQLException {
        String sektor = rs.getString("sektor");
        String orgtilknytningType = rs.getString("orgtilknytning_type");
        return new OrgtilknytningState(
            rs.getLong("id"),
            rs.getLong("test_sett_id"),
            new OrgtilknytningState.Data(
                rs.getLong("sort_idx"),
                rs.getObject("state_timestamp", java.time.LocalDateTime.class),
                rs.getString("fid"),
                rs.getString("navident"),
                rs.getString("org_enhet_id"),
                sektor == null ? null : Sektor.valueOf(sektor),
                rs.getBoolean("er_daglig_oppfolging"),
                rs.getObject("gyldig_fom", java.time.LocalDate.class),
                rs.getObject("gyldig_tom", java.time.LocalDate.class),
                orgtilknytningType == null ? null : Tilknytningtype.valueOf(orgtilknytningType),
                rs.getString("primary_fysisk_brukersted_nummer"),
                JdbcArrays.readTextArray(rs, "additional_fysiske_brukersteder_nummer"),
                rs.getString("masseflyttet_fra_fid"),
                rs.getString("state_sist_endret_av")
            ),
            Audit.mapAudit(rs)
        );
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
