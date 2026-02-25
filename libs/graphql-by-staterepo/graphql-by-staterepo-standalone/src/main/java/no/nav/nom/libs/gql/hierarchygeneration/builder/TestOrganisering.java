package no.nav.nom.libs.gql.hierarchygeneration.builder;

import java.time.LocalDate;

public class TestOrganisering {
    public final LocalDate fom;
    public final LocalDate tom;

    // changed post construction
    TestOrgNode nodeAbove;

    private TestOrganisering(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public static TestOrganisering standard() {
        return new TestOrganisering(LocalDate.now().minusDays(5), null);
    }
}
