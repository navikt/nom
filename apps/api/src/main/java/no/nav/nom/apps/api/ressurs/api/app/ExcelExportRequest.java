package no.nav.nom.apps.api.ressurs.api.app;

import java.util.List;

public record ExcelExportRequest(
    String topOrgEnhetId,
    List<String> navidenter
) {
}
