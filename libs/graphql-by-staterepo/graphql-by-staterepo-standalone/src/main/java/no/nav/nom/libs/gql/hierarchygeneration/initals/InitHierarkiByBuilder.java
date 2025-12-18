package no.nav.nom.libs.gql.hierarchygeneration.initals;


import no.nav.nom.libs.gql.hierarchygeneration.builder.InitHierarkiIdGenerator;
import no.nav.nom.libs.gql.hierarchygeneration.builder.TestOrgLeder;
import no.nav.nom.libs.gql.hierarchygeneration.builder.TestOrgNode;
import no.nav.nom.libs.gql.hierarchygeneration.builder.TestOrganisering;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;

import java.util.List;
import java.util.stream.Stream;

public class InitHierarkiByBuilder {


    public final InitHierarkiIdGenerator idGen = new InitHierarkiIdGenerator();

    public final OrgEnhetState ENHET_NAV = idGen.orgEnhetStateBuilder("NAV").nomNivaa(null).build();

    public final OrgEnhetState ENHET_ARB_OG_TJEN = idGen.orgEnhetStateBuilder("arb_og_tjen").nomNivaa(NomNivaa.LINJEENHET).build();

    public final OrgEnhetState ENHET_NORDLAND = idGen.orgEnhetStateBuilder("Nordland")
        .nomNivaa(NomNivaa.DRIFTSENHET)
        .orgEnhetsType(OrgEnhetsType.FYLKE)
        .build();

    public final OrgEnhetState ENHET_SORTLAND = idGen.orgEnhetStateBuilder("Sortland").build();
    public final OrgEnhetState ENHET_SORTLAND_7S = idGen.orgEnhetStateBuilder("Sortland 7-siffer").nomNivaa(null).orgEnhetsType(OrgEnhetsType.NAV_KONTOR).build();

    public final OrgEnhetState ENHET_NARVIK = idGen.orgEnhetStateBuilder("Narvik").build();
    public final OrgEnhetState ENHET_NARVIK_7S = idGen.orgEnhetStateBuilder("Narvik 7-siffer").nomNivaa(null).orgEnhetsType(OrgEnhetsType.NAV_KONTOR).build();




    public final RessursState LEDER_NAV = idGen.ressursStateBuilder("NAV-leder").build();
    public final RessursState LEDER_ARB_OG_TJEN = idGen.ressursStateBuilder("Arb-og-tjen-leder").build();
    public final RessursState LEDER_NORDLAND = idGen.ressursStateBuilder("Nordland-leder").build();
    public final RessursState LEDER_SORTLAND = idGen.ressursStateBuilder("Sortland-leder").build();
    public final RessursState LEDER_SORTLAND_7S = idGen.ressursStateBuilder("Sortland-7siffer-leder").build();
    public final RessursState LEDER_NARVIK = idGen.ressursStateBuilder("Narvik-leder","A123456").build();


    public final List<RessursState> ANSATTE_SORTLAND = Stream.of(1, 2, 3, 4, 5, 6, 7)
        .map(it -> idGen.ressursStateBuilder("Sortland ressurs " + it).build())
        .toList();
    public final List<RessursState> ANSATTE_NARVIK = Stream.of(1, 2, 3, 4).map(it -> idGen.ressursStateBuilder("Narvik ressurs " + it).build()
    ).toList();


    public final TestOrgNode testHierarki = TestOrgNode.createRootNode(
        ENHET_NAV,
        TestOrgLeder.simpleList(LEDER_NAV),
        List.of(
            TestOrgNode.createAggregateNode(
                ENHET_ARB_OG_TJEN,
                LEDER_ARB_OG_TJEN,
                List.of(
                    TestOrgNode.createAggregateNode(
                        ENHET_NORDLAND,
                        LEDER_NORDLAND,
                        List.of(
                            TestOrgNode.createAggregateNode(
                                ENHET_SORTLAND,
                                LEDER_SORTLAND,
                                List.of(
                                    TestOrgNode.createOrgenhet(
                                        TestOrganisering.standard(),
                                        ENHET_SORTLAND_7S,
                                        LEDER_SORTLAND_7S,
                                        ANSATTE_SORTLAND
                                    )
                                )
                            ),
                            TestOrgNode.createAggregateNode(
                                ENHET_NARVIK,
                                LEDER_NARVIK,
                                List.of(
                                    TestOrgNode.createOrgenhet(
                                        TestOrganisering.standard(),
                                        ENHET_NARVIK_7S,
                                        LEDER_NARVIK,
                                        ANSATTE_NARVIK
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    );



    static void main(){
        var stateObjectsFromHierarchy = new InitHierarkiByBuilder().testHierarki.generateStateObjects();
        InitHierarkiUtils.writeStateObjectsToTmpFile(stateObjectsFromHierarchy);
    }

}
