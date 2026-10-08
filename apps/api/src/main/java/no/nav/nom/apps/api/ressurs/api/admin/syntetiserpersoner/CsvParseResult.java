package no.nav.nom.apps.api.ressurs.api.admin.syntetiserpersoner;

import java.util.List;

public record CsvParseResult(
    List<RessursEndringCsvRow> rows,
    List<String> warnings
) {
}
