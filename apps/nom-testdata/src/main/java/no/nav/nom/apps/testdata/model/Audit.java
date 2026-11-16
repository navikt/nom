package no.nav.nom.apps.testdata.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public record Audit(
    LocalDateTime opprettet,
    String opprettetAv,
    LocalDateTime sistEndret,
    String sistEndretAv
) {

    public record Endring(
        LocalDateTime tidspunkt,
        String bruker
    ) {}

    public static Audit mapAudit(ResultSet rs) throws SQLException {
        return new Audit(
            rs.getObject("opprettet", java.time.LocalDateTime.class), rs.getString("opprettet_av"),
            rs.getObject("sist_endret", java.time.LocalDateTime.class), rs.getString("sist_endret_av")
        );
    }
}
