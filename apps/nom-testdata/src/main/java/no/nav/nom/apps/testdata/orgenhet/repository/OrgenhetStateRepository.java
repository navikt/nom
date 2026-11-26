package no.nav.nom.apps.testdata.orgenhet.repository;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetState;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class OrgenhetStateRepository {
    private static final String COLUMNS =
        """
        id, test_sett_id, sort_idx, state_timestamp,
        fid, orgenhet_id, navn, gyldig_fom, gyldig_tom, orgenhets_type, nom_nivaa,
        unique_agresso_id, agresso_id, agresso_org_nivaa, agresso_orgenhet_type,
        tilgangsenhet_id, kostnadssted_id,
        opprettet, opprettet_av, sist_endret, sist_endret_av
        """;
    private static final RowMapper<OrgenhetState> ROW_MAPPER = OrgenhetStateRepository::mapRow;

    private final JdbcClient jdbcClient;

    public OrgenhetStateRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public OrgenhetState create(long testSettId, OrgenhetState.Data data, Audit.Endring endring) {
        String sql =
            """
            INSERT INTO orgenhet_state (
                test_sett_id, sort_idx, state_timestamp, fid, orgenhet_id, navn,
                gyldig_fom, gyldig_tom, orgenhets_type, nom_nivaa, unique_agresso_id, agresso_id, agresso_org_nivaa,
                agresso_orgenhet_type, tilgangsenhet_id, kostnadssted_id, opprettet, opprettet_av, sist_endret, sist_endret_av
            )
            VALUES (
                :testSettId, :sortIdx, :stateTimestamp, :fid, :orgenhetId, :navn,
                :gyldigFom, :gyldigTom, :orgenhetsType, :nomNivaa, :uniqueAgressoId, :agressoId, :agressoOrgNivaa,
                :agressoOrgenhetType, :tilgangsenhetId, :kostnadsstedId, :opprettet, :opprettetAv, :sistEndret, :sistEndretAv
            )
            RETURNING %s
            """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("testSettId", testSettId)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("orgenhetId", data.orgenhetId())
            .param("navn", data.navn())
            .param("gyldigFom", data.gyldigFom())
            .param("gyldigTom", data.gyldigTom())
            .param("orgenhetsType", enumName(data.orgenhetsType()))
            .param("nomNivaa", enumName(data.nomNivaa()))
            .param("uniqueAgressoId", data.uniqueAgressoId())
            .param("agressoId", data.agressoId())
            .param("agressoOrgNivaa", data.agressoOrgNivaa())
            .param("agressoOrgenhetType", data.agressoOrgenhetType())
            .param("tilgangsenhetId", data.tilgangsenhetId())
            .param("kostnadsstedId", data.kostnadsstedId())
            .param("opprettet", endring.tidspunkt())
            .param("opprettetAv", endring.bruker())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .query(ROW_MAPPER)
            .single();
    }

    public Optional<OrgenhetState> findById(long testSettId, long id) {
        return jdbcClient.sql("SELECT %s FROM orgenhet_state WHERE test_sett_id = :testSettId AND id = :id".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public List<OrgenhetState> findAll(long testSettId) {
        return jdbcClient.sql("SELECT %s FROM orgenhet_state WHERE test_sett_id = :testSettId ORDER BY sort_idx".formatted(COLUMNS))
            .param("testSettId", testSettId)
            .query(ROW_MAPPER)
            .list();
    }

    public List<OrgenhetState> findByTestSetIdAndOrgUnitId(long testSettId, String orgenhetId) {
        return jdbcClient.sql("""
                              SELECT %s
                              FROM orgenhet_state
                              WHERE test_sett_id = :testSettId
                              AND orgenhet_id = :orgenhetId
                              ORDER BY sort_idx
                              """.formatted(COLUMNS)
            )
            .param("testSettId", testSettId)
            .param("orgenhetId", orgenhetId)
            .query(ROW_MAPPER)
            .list();
    }

    public Optional<OrgenhetState> update(long testSettId, long id, OrgenhetState.Data data, Audit.Endring endring) {
        String sql = """
                     UPDATE orgenhet_state
                     SET sort_idx = :sortIdx, state_timestamp = :stateTimestamp,
                         fid = :fid, orgenhet_id = :orgenhetId, navn = :navn,
                         gyldig_fom = :gyldigFom, gyldig_tom = :gyldigTom, orgenhets_type = :orgenhetsType, nom_nivaa = :nomNivaa,
                         unique_agresso_id = :uniqueAgressoId, agresso_id = :agressoId, agresso_org_nivaa = :agressoOrgNivaa,
                         agresso_orgenhet_type = :agressoOrgenhetType, tilgangsenhet_id = :tilgangsenhetId, kostnadssted_id = :kostnadsstedId,
                         sist_endret = :sistEndret, sist_endret_av = :sistEndretAv
                     WHERE test_sett_id = :testSettId
                     AND id = :id
                     RETURNING %s
                     """.formatted(COLUMNS);
        return jdbcClient.sql(sql)
            .param("sortIdx", data.sortIdx())
            .param("stateTimestamp", data.stateTimestamp())
            .param("fid", data.fid())
            .param("orgenhetId", data.orgenhetId())
            .param("navn", data.navn())
            .param("gyldigFom", data.gyldigFom())
            .param("gyldigTom", data.gyldigTom())
            .param("orgenhetsType", enumName(data.orgenhetsType()))
            .param("nomNivaa", enumName(data.nomNivaa()))
            .param("uniqueAgressoId", data.uniqueAgressoId())
            .param("agressoId", data.agressoId())
            .param("agressoOrgNivaa", data.agressoOrgNivaa())
            .param("agressoOrgenhetType", data.agressoOrgenhetType())
            .param("tilgangsenhetId", data.tilgangsenhetId())
            .param("kostnadsstedId", data.kostnadsstedId())
            .param("sistEndret", endring.tidspunkt())
            .param("sistEndretAv", endring.bruker())
            .param("testSettId", testSettId)
            .param("id", id)
            .query(ROW_MAPPER)
            .optional();
    }

    public int delete(long testSettId, long id) {
        return jdbcClient.sql("DELETE FROM orgenhet_state WHERE test_sett_id = :testSettId AND id = :id")
            .param("testSettId", testSettId)
            .param("id", id)
            .update();
    }

    public int deleteAllByTestSetId(long testSettId) {
        return jdbcClient.sql("DELETE FROM orgenhet_state WHERE test_sett_id = :testSettId")
            .param("testSettId", testSettId)
            .update();
    }

    public int updateSortIndex(long testSettId, long id, long sortIdx, Audit.Endring endring) {
        return jdbcClient.sql("""
                              UPDATE orgenhet_state
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
        jdbcClient.sql("SET CONSTRAINTS uq_orgenhet_state_sort_idx DEFERRED").update();
    }

    private static OrgenhetState mapRow(ResultSet rs, int rowNum) throws SQLException {
        String orgenhetsType = rs.getString("orgenhets_type");
        String nomNivaa = rs.getString("nom_nivaa");
        return new OrgenhetState(
            rs.getLong("id"),
            rs.getLong("test_sett_id"),
            new OrgenhetState.Data(
                rs.getLong("sort_idx"),
                rs.getObject("state_timestamp", java.time.LocalDateTime.class),
                rs.getString("fid"),
                rs.getString("orgenhet_id"),
                rs.getString("navn"),
                rs.getObject("gyldig_fom", java.time.LocalDate.class),
                rs.getObject("gyldig_tom", java.time.LocalDate.class),
                orgenhetsType == null ? null : OrgEnhetsType.valueOf(orgenhetsType),
                nomNivaa == null ? null : NomNivaa.valueOf(nomNivaa),
                rs.getString("unique_agresso_id"),
                rs.getString("agresso_id"),
                rs.getString("agresso_org_nivaa"),
                rs.getString("agresso_orgenhet_type"),
                rs.getString("tilgangsenhet_id"),
                rs.getString("kostnadssted_id")
            ),
            Audit.mapAudit(rs)
        );
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
