package no.nav.nom.apps.testdata.model;

import java.util.List;

public record KomplettTestSett(
    TestSett testSett,
    List<OrgenhetState> states,
    List<OrgenhetEvtsrcMsg> meldinger,
    List<RessursState> ressursStates,
    List<RessursEvtsrcMsg> ressursMeldinger,
    List<OrganiseringState> organiseringStates,
    List<OrganiseringEvtsrcMsg> organiseringMeldinger,
    List<OrgEnhetLederState> orgenhetLederStates,
    List<OrgEnhetLederEvtsrcMsg> orgenhetLederMeldinger,
    List<OrgtilknytningState> orgtilknytningStates,
    List<OrgtilknytningEvtsrcMsg> orgtilknytningMeldinger
) {
    @Override
    public String toString() {
        return testSett == null ? null : testSett.toString();
    }
}
