package no.nav.nom.apps.admin.sanitycheck.remedycompare;

import no.nav.nom.contract.nom.state.RessursState;

public record RemedyCompareResult(
    boolean mastretINom,
    RessursState ressursState,
    String nomRemedyEnhet,
    ImportedRemedyUser importedRemedyUser
) {}
