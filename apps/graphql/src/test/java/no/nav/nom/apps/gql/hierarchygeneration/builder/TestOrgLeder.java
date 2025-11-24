package no.nav.nom.apps.gql.hierarchygeneration.builder;

import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.List;

public class TestOrgLeder {
    RessursState leder;
    LocalDate startDato;
    LocalDate sluttDato;

    public TestOrgLeder(RessursState leder, LocalDate startDato, LocalDate sluttDato) {
        this.leder = leder;
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public static List<TestOrgLeder> simpleList(RessursState leder) {
        return List.of(new TestOrgLeder(
            leder,
            LocalDate.now().minusDays(5),
            LocalDate.now().plusDays(500)
        ));
    }
}
