package no.nav.nom.apps.testdata.model;

import java.util.List;

public record KomplettTestSett(
    TestSett testSett,
    List<OrgenhetState> states,
    List<OrgenhetEvtsrcMsg> meldinger
) {}
