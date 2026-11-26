package no.nav.nom.apps.testdata.testsett;

import no.nav.nom.apps.testdata.organisering.OrganiseringEvtsrcMsg;
import no.nav.nom.apps.testdata.organisering.OrganiseringState;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetEvtsrcMsg;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetState;
import no.nav.nom.apps.testdata.orgenhetleder.OrgEnhetLederEvtsrcMsg;
import no.nav.nom.apps.testdata.orgenhetleder.OrgEnhetLederState;
import no.nav.nom.apps.testdata.orgtilknytning.OrgtilknytningEvtsrcMsg;
import no.nav.nom.apps.testdata.orgtilknytning.OrgtilknytningState;
import no.nav.nom.apps.testdata.ressurs.RessursEvtsrcMsg;
import no.nav.nom.apps.testdata.ressurs.RessursState;

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
