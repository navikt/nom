package no.nav.nom.apps.gql.hierarchygeneration.builder;

import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestOrgNode {

    TestOrganisering testOrganisering;
    OrgEnhetState orgState;
    List<TestOrgLeder> ledere;
    List<TestOrgNode> underNoder;
    List<RessursState> ressurser;

    private TestOrgNode(
        TestOrganisering testOrganisering,
        OrgEnhetState orgState,
        List<TestOrgLeder> ledere,
        List<RessursState> ressurser,
        List<TestOrgNode> underNoder
    ) {
        this.testOrganisering = testOrganisering;
        this.orgState = orgState;
        this.ledere = ledere;
        this.underNoder = underNoder;

        // must be changed to contain dedicated node to allow for custom from/to/DOL etc info
        this.ressurser = new ArrayList<RessursState>(ressurser);
    }

    public static TestOrgNode createRootNode(OrgEnhetState orgState, List<TestOrgLeder> ledere, List<TestOrgNode> underNoder) {
        return new TestOrgNode(null, orgState, ledere, List.of(), underNoder);

    }

    public static TestOrgNode createOrgenhet(TestOrganisering ot, OrgEnhetState orgState, RessursState leder, List<RessursState> ressurser) {
        return new TestOrgNode(ot, orgState, TestOrgLeder.simpleList(leder), ressurser, List.of());
    }

    public static TestOrgNode createOrgenhet(
        TestOrganisering testOrganisering,
        OrgEnhetState orgState,
        List<TestOrgLeder> ledere,
        List<RessursState> ressurser
    ) {
        return new TestOrgNode(testOrganisering, orgState, ledere, ressurser, List.of());
    }

    public static TestOrgNode createAggregateNode(
        TestOrganisering testOrganisering,
        OrgEnhetState orgState,
        List<TestOrgLeder> ledere,
        List<TestOrgNode> underNoder
    ) {
        return new TestOrgNode(testOrganisering, orgState, ledere, List.of(), underNoder);
    }

    public static TestOrgNode createAggregateNode(
        TestOrganisering testOrganisering,
        OrgEnhetState orgState,
        RessursState leder,
        List<TestOrgNode> underNoder
    ) {
        return new TestOrgNode(testOrganisering, orgState, TestOrgLeder.simpleList(leder), List.of(), underNoder);
    }

    public static TestOrgNode createAggregateNode(OrgEnhetState orgState, RessursState leder, List<TestOrgNode> underNoder) {
        return new TestOrgNode(
            TestOrganisering.standard(),
            orgState,
            TestOrgLeder.simpleList(leder),
            List.of(),
            underNoder
        );
    }


    public List<Object> generateStateObjects(){
        return this.generateStateObjects(null);
    }

    private List<Object> generateStateObjects(TestOrgNode over) {
        var out = new ArrayList<Object>();
        out.add(this.orgState);

        { // leder state
            for (var ledernode : this.ledere) {
                // make sure to upsert leder ressursstate itself
                out.add(ledernode.leder);
                var oeLEderState = new OrgEnhetLederState(
                    "fid-" + ledernode.leder.navident() + "-" + this.orgState.id(),
                    ledernode.leder.navident(),
                    this.orgState.id(),
                    ledernode.startDato,
                    ledernode.sluttDato,
                    null
                );
                out.add(oeLEderState);
                ressurser.add(ledernode.leder);
            }
        }
        { // organisering til node over
            if (this.testOrganisering != null) {
                this.testOrganisering.nodeAbove = over;
                var organisering = OrganiseringState.builder()
                    .fid("fid-" + this.orgState.id() + " under " + testOrganisering.nodeAbove.orgState.id())
                    .gyldigFom(this.testOrganisering.fom)
                    .gyldigTom(this.testOrganisering.tom)
                    .navn(this.orgState.navn())
                    .navnOver(this.testOrganisering.nodeAbove.orgState.navn())
                    .id(this.orgState.id())
                    .idOver(this.testOrganisering.nodeAbove.orgState.id())
                    .build();

                out.add(organisering);
            }
        }

        { // underenhetene
            for (var undernode : this.underNoder) {
                out.add(undernode.orgState);
                out.addAll(undernode.generateStateObjects(this));
            }
        }

        { // ressursene
            out.addAll(this.ressurser);
            var orgtilknytninger = this.ressurser.stream().map(x -> OrgtilknytningState.builder()
                .fid("fid-"+x.fid())
                .navident(x.navident())
                .orgEnhetId(this.orgState.id())
                .erDagligOppfolging(true)
                .gyldigFom(LocalDate.now().minusDays(5))
                .gyldigTom(null)
                .orgtilknytningType(Tilknytningtype.FAST)
                .build()
            ).toList();
            out.addAll(orgtilknytninger);
        }


        return out;
    }

}
