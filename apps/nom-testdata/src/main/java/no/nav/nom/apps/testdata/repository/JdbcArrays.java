package no.nav.nom.apps.testdata.repository;

import org.springframework.jdbc.support.SqlArrayValue;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class JdbcArrays {
    private JdbcArrays() {
    }

    public static SqlArrayValue textArray(List<String> value) {
        return value == null ? null : new SqlArrayValue("text", value.toArray());
    }

    public static List<String> readTextArray(ResultSet rs, String column) throws SQLException {
        Array sqlArray = rs.getArray(column);
        if (sqlArray == null) {
            return null;
        }

        try {
            String[] values = (String[]) sqlArray.getArray();
            return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(values)));
        } finally {
            sqlArray.free();
        }
    }
}
